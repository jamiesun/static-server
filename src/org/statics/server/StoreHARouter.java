package org.statics.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.Startable;
import org.statics.common.SystemConfig;
import org.statics.common.Utils;
import org.statics.service.service.ConfigService;
import org.xsocket.connection.NonBlockingConnectionPool;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.rep.NodeType;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.ReplicationGroup;
import com.sleepycat.je.rep.ReplicationNode;
import com.sleepycat.je.rep.UnknownMasterException;
import com.sleepycat.je.rep.monitor.GroupChangeEvent;
import com.sleepycat.je.rep.monitor.JoinGroupEvent;
import com.sleepycat.je.rep.monitor.LeaveGroupEvent;
import com.sleepycat.je.rep.monitor.Monitor;
import com.sleepycat.je.rep.monitor.MonitorChangeListener;
import com.sleepycat.je.rep.monitor.NewMasterEvent;

/**
 * 存储服务路由
 *
 */
public class StoreHARouter implements Startable,SystemConfig
{

    private Monitor monitor;
    
    private ReplicationNode master;
    
    /**
     * 启动监听服务异常重试间隔时间（毫秒）
     */
    private final int retrySleepMs = 2000;
    
    /**
     * 启动监听服务有效时间，必须在此时间内启动，否则放弃
     */
    private final int retryPeriodMins = 5;
    
    /**
     * 启动监听服务异常重试次数
     */
    private int maxRetries = (retryPeriodMins*60*1000)/retrySleepMs;
    
    private static final ReplicationConfig repConfig = new ReplicationConfig();
    
    private final static Log log = LogFactory.getLog(StoreHARouter.class);
    
    private final Object groupLock = new Object();
    
    private final Object masterLock = new Object();

    private volatile String masterName = null;

    private InetSocketAddress nodeMasterAddress;
    
    private static final Map<String, InetSocketAddress> nodeAddressMapping =
        new HashMap<String, InetSocketAddress>();

    private static final List<InetSocketAddress> activeServerAddresses = 
        new LinkedList<InetSocketAddress>();
    
    private NonBlockingConnectionPool cpool;
    
    private ConfigService configService ;
    
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    
    public  Map<String, InetSocketAddress> getNodeAddressMapping()
    {
        return Collections.unmodifiableMap(nodeAddressMapping);

    }
    
    /**
     * 获得激活节点服务地址缓存列表
     * @return
     */
    public List<InetSocketAddress> getActiveServerAddresses()
    {
        return Collections.unmodifiableList(activeServerAddresses);
    }
    
    /**
     * 获得主节点名称
     */
    public String getMasterName()
    {
        return masterName;
    }
    
    public boolean checkMaster()
    {
        return (masterName+"").equals(configService.getString("node.name"));
    }
    
    /**
     * 获得主节点服务地址
     * @return
     */
    public InetSocketAddress getNodeMasterAddress()
    {
        return nodeMasterAddress;
    }
    

    
    /**
     * 节点的服务接口地址, 端口（tcp.port） = 存储服务同步端口 - 偏移值（NODE_PORT_DISPLACEMENT）
     * @param nodeAddress
     * @return
     */
    public  InetSocketAddress getNodeServerAddress(InetSocketAddress nodeAddress)
    {
        return new InetSocketAddress(nodeAddress.getHostName(),nodeAddress.getPort()-NODE_PORT_DISPLACEMENT);
    }
    
    /**
     * 初始化复制群组，缓存所有节点的服务接口地址 
     * @param electableNodes
     */
    private void initGroup(Set<ReplicationNode> electableNodes) {
        log.info("-当前组大小 : " + electableNodes.size());
        synchronized (groupLock) {
            nodeAddressMapping.clear();
            for (ReplicationNode node : electableNodes) {
                nodeAddressMapping.put(node.getName(),getNodeServerAddress(node.getSocketAddress()));
            }
        }
    }
    
    /**
     * 更新当前复制群组主节点地址信息
     * @param newMasterName
     * @param masterNodeAddress
     */
    private void updateMaster(String newMasterName,InetSocketAddress masterNodeAddress)
    {
        if(log.isInfoEnabled())
            log.info("-当前主节点<MASTER>: " + newMasterName);
        synchronized (masterLock)
        {
            masterName = newMasterName;
            nodeMasterAddress = getNodeServerAddress(masterNodeAddress);
        }
    }

    /**
     * 当复制群组内变化时产生主节点选举时更新群组
     */
    private void updateGroup(GroupChangeEvent event) {
        synchronized (groupLock) {
            ReplicationGroup group = event.getRepGroup();
            String nodeName = event.getNodeName();

            switch (event.getChangeType()) {
                case REMOVE:
                    if(log.isInfoEnabled())
                        log.info("-节点( " + nodeName + ")从组中删除");
                    removeActiveNode(nodeName);
                    nodeAddressMapping.remove(nodeName);
                    break;
                case ADD:
                    if(log.isInfoEnabled())
                        log.info("-节点( " + nodeName + ")加入组");
                    ReplicationNode node = group.getMember(nodeName);
                    nodeAddressMapping.put(nodeName,node.getSocketAddress());
                    break;
                default:
                    throw new IllegalStateException("-未知事件: " + event.getChangeType());
            }
        }
    }
       
    /**
     * 当一个新的节点加入时更新激活节点缓存列表
     * @param event
     */
    private void addActiveNode(JoinGroupEvent event) {
        if(log.isInfoEnabled())
            log.info("-节点(" + event.getNodeName()
                + ")加入组， 加入时间: "+ event.getJoinTime()
                +",当前主节点<MASTER>:"+event.getMasterName());
        synchronized (groupLock) {
            InetSocketAddress address = 
                nodeAddressMapping.get(event.getNodeName());
            for (InetSocketAddress addr : activeServerAddresses)
            {
                if(addr.getHostName().equals(address.getHostName())&&addr.getPort()==address.getPort())
                    return;
            }
            activeServerAddresses.add(getNodeServerAddress(address));
        }
    }

    /**
     * 当一个节点离开复制群组时更新激活节点缓存列表
     * @param event
     */
    private void removeActiveNode(LeaveGroupEvent event) {   
        if(log.isInfoEnabled())
            log.info("-节点("+ event.getNodeName() 
                + ")离开组("+event.getLeaveReason()
                +"),加入时间:" + event.getJoinTime()
                +",离开时间:" + event.getLeaveTime()
                +",当前主节点<MASTER>:"+event.getMasterName());
        synchronized (groupLock) {
            removeActiveNode(event.getNodeName());
        }
    }

    /**
     * 从激活节点缓存列表删除一个节点
     * @param nodeName
     */
    private void removeActiveNode(String nodeName) {
        InetSocketAddress address =
            nodeAddressMapping.get(nodeName);
        activeServerAddresses.remove(address);
    }
    
    /**
     * 节点变化监听
     */
    class RouterChangeListener implements MonitorChangeListener {

        public void notify(NewMasterEvent newMasterEvent) {
            updateMaster(newMasterEvent.getNodeName(),
                         newMasterEvent.getSocketAddress());
        }

        public void notify(GroupChangeEvent groupChangeEvent) {
            updateGroup(groupChangeEvent);
        }

        public void notify(JoinGroupEvent joinGroupEvent) {
            addActiveNode(joinGroupEvent);
        }

        public void notify(LeaveGroupEvent leaveGroupEvent) {
            removeActiveNode(leaveGroupEvent);
        }
    }
    
    /**
     * 启动存储服务路由
     */
    public void start()
    {
        int helperPort = configService.getInt("tcp.port")+(NODE_PORT_DISPLACEMENT);
        int monitorPort = configService.getInt("tcp.port")+(NODE_PORT_DISPLACEMENT*2);
        String helperAddress = Utils.getLocalIP()+":"+helperPort;;
        String  monitorAddress = Utils.getLocalIP()+":"+monitorPort;

        repConfig.setNodeName(configService.getString("node.name")+"Monitor");
        repConfig.setGroupName(configService.getString("node.group"));
        repConfig.setNodeHostPort(monitorAddress);
        repConfig.setHelperHosts(helperAddress);
        repConfig.setNodeType(NodeType.MONITOR);
        monitor = new Monitor(repConfig);
        
        while (true) {
            try {
                master = monitor.register();
                ReplicationGroup repGroup = monitor.getGroup();
                initGroup(repGroup.getElectableNodes());
                updateMaster(master.getName(), master.getSocketAddress());
                monitor.startListener(new RouterChangeListener());
                cpool = new NonBlockingConnectionPool();
                if(log.isInfoEnabled())
                    log.info("start StoreHARouter at "+monitorAddress);
                break;
            } catch (UnknownMasterException unknownMasterException) {
                if (maxRetries-- == 0) {
                    /* Don't have a functioning group. */
                    throw unknownMasterException;
                }
                log.error(new Date() +" 正在等待一个新的主节点<MASTER>加入." +
                    unknownMasterException);
                try
                {
                    Thread.sleep(retrySleepMs);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            catch (DatabaseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
    }

    public void stop()
    {
        try
        {
            monitor.shutdown();
            log.info("StoreHARouter has been shutdown! ");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        
    }
    
    

}
