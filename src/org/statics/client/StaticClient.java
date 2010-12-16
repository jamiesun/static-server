package org.statics.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.management.JMException;

import org.statics.common.StaticObject;
import org.statics.common.SystemConfig;
import org.statics.common.Utils;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;
import org.xsocket.connection.multiplexed.IBlockingPipeline;
import org.xsocket.connection.multiplexed.IMultiplexedConnection;
import org.xsocket.connection.multiplexed.INonBlockingPipeline;
import org.xsocket.connection.multiplexed.IPipelineDataHandler;
import org.xsocket.connection.multiplexed.MultiplexedConnection;


public class StaticClient implements SystemConfig{
    private NonBlockingConnectionPool cpool;
	private int timeout;
	private int limit = 10*1024*1024;
	private InetSocketAddress masterNode;
	private List<InetSocketAddress> serverNodes = new ArrayList<InetSocketAddress>();
	private static final Logger log = Logger.getLogger("StaticClient");
	
	public StaticClient(String[] hosts, int timeoutSec, int poolSize) {
	    //设置客户端连接使用底层直接缓冲读数据
	    System.setProperty("org.xsocket.connection.client.readbuffer.usedirect", "true");
	    for (String host : hosts)
        {
            String[] hp = host.split(":");
            serverNodes.add(new InetSocketAddress(hp[0],Integer.parseInt(hp[1])));
        }
	    masterNode = serverNodes.get(0);
		this.timeout = timeoutSec*1000;
		cpool = new NonBlockingConnectionPool();
		cpool.setMaxActive(poolSize);
		cpool.setAcquireTimeoutMillis(5*1000);
		cpool.setWorkerpool(Executors.newCachedThreadPool(new XThreadFactory(false)));
		
		try
        {
            ConnectionUtils.registerMBean(this.cpool);
        }
        catch (JMException e)
        {
            e.printStackTrace();
        }
	}

    public void close()
    {
    	try {
    	    cpool.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public synchronized void ping()
    {
        log.info("ping...");
        for (InetSocketAddress node : serverNodes)
        {
            try
            {
                INonBlockingConnection nativeCon = new NonBlockingConnection(node);
                IMultiplexedConnection multiplexedCon = new MultiplexedConnection(nativeCon);
                String controlPipelineId = multiplexedCon.createPipeline();   
                IBlockingPipeline controlPipeline = multiplexedCon.getBlockingPipeline(controlPipelineId); 
                controlPipeline.setConnectionTimeoutMillis(timeout);
                controlPipeline.setAutoflush(false);
                controlPipeline.markWritePosition();
                
                try
                {
                    controlPipeline.write(IDENTIFIER_PING);
                    controlPipeline.flush();
                    int idf = controlPipeline.readInt();
                    if(idf!=IDENTIFIER_PING_ACK)
                        continue;
                    
                    int resp = controlPipeline.readInt();
                    if(resp==FAILURE)
                        continue;
                    
                    String ip = Utils.long2ip(controlPipeline.readLong());
                    int port = controlPipeline.readInt();
                    
                    if(masterNode==null
                        ||(!masterNode.getAddress().getHostAddress().equals(ip)||masterNode.getPort()!=port))
                    {
                        masterNode = new InetSocketAddress(ip,port);
                    }

                    log.info("当前可写服务节点:"+masterNode);
                    return;
                }
                finally
                {
                    controlPipeline.close();
                    multiplexedCon.close();
                } 
            }
            catch (Exception e)
            {
                log.warning(node+"ping error "+e);
                continue;
            }
        }
    }
	/**
	 * 同步新增文件
	 * @param sid
	 * @param contentType
	 * @param metadata
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public int add(StaticObject sto) throws Exception
	{
	   if(sto.getData().length>limit)
	   {
	       throw new Exception("数据大小超过限制，最大("+limit/1024+"KB)");
	   }
       INonBlockingConnection nativeCon = cpool.getNonBlockingConnection(masterNode);
       IMultiplexedConnection multiplexedCon = (IMultiplexedConnection) nativeCon.getAttachment();
       if(multiplexedCon==null)
       {
           multiplexedCon =  new MultiplexedConnection(nativeCon);
           nativeCon.setAttachment(multiplexedCon);
       }
       String controlPipelineId = multiplexedCon.createPipeline();   
       IBlockingPipeline controlPipeline = multiplexedCon.getBlockingPipeline(controlPipelineId); 
       controlPipeline.setConnectionTimeoutMillis(timeout);
       controlPipeline.setAutoflush(false);
       controlPipeline.markWritePosition();
       try
       {
           controlPipeline.write(IDENTIFIER_ADD);
           controlPipeline.write(sto.packer());
           controlPipeline.flush();
           
           int idf = controlPipeline.readInt();
           if(idf!=IDENTIFIER_ADD_ACK)
               return FAILURE;
           int resp = controlPipeline.readInt();
           if(resp==NOT_MASTER)
           {
               ping();
               return add(sto);
           }
           return resp;
       }
       finally
       {
            controlPipeline.close();
            nativeCon.close();
       }

    		
     }
	
	/**
	 * 异步新增文件
	 * @param sid
	 * @param contentType
	 * @param metadata
	 * @param data
	 * @param hdl
	 * @throws Exception
	 */
    public void add(final StaticObject sto,final ClientHandler hdl) throws Exception
    {
        if(sto.getData().length>limit)
        {
            throw new Exception("数据大小超过限制，最大("+limit/1024+"KB)");
        }
        final INonBlockingConnection nativeCon = cpool.getNonBlockingConnection(masterNode);
        IMultiplexedConnection multiplexedCon = (IMultiplexedConnection) nativeCon.getAttachment();
        if(multiplexedCon==null)
        {
            multiplexedCon =  new MultiplexedConnection(nativeCon);
            nativeCon.setAttachment(multiplexedCon);
        }
        String controlPipelineId = multiplexedCon.createPipeline();   
        INonBlockingPipeline controlPipeline = multiplexedCon.getNonBlockingPipeline(controlPipelineId); 
        controlPipeline.setConnectionTimeoutMillis(timeout);
        controlPipeline.setHandler(new IPipelineDataHandler(){
            @Execution(Execution.MULTITHREADED)
            public boolean onData(INonBlockingPipeline pipe)
                throws IOException, BufferUnderflowException,
                MaxReadSizeExceededException
            {
                if(pipe.isOpen())
                {
                    int idf = pipe.readInt();
                    /** 如果当前主机不可写，则ping一次，再重调用 **/
                    if(idf==SystemConfig.NOT_MASTER)
                    {
                        pipe.close();
                        nativeCon.close();
                        try{
                            ping();
                            add(sto,hdl);
                        }catch (Exception e){}
                        return true;
                    }
                    if(idf!=SystemConfig.IDENTIFIER_ADD_ACK)
                    {
                        hdl.onResponse(SystemConfig.FAILURE);
                        return true;
                    }
                    hdl.onResponse(pipe.readInt());
                    pipe.close();
                    nativeCon.close();
                }
                return true;
            }
            
        });
        controlPipeline.setAutoflush(false);
        controlPipeline.markWritePosition();
        controlPipeline.write(IDENTIFIER_ADD);
        controlPipeline.write(sto.packer());
        controlPipeline.flush();
    }
    
	   

	public int del(String sid) throws Exception
	{
	    INonBlockingConnection nativeCon = cpool.getNonBlockingConnection(masterNode);
        IMultiplexedConnection multiplexedCon = (IMultiplexedConnection) nativeCon.getAttachment();
        if(multiplexedCon==null)
        {
            multiplexedCon =  new MultiplexedConnection(nativeCon);
            nativeCon.setAttachment(multiplexedCon);
        }
        String controlPipelineId = multiplexedCon.createPipeline();   
        IBlockingPipeline controlPipeline = multiplexedCon.getBlockingPipeline(controlPipelineId); 
        controlPipeline.setConnectionTimeoutMillis(timeout);
        if(sid==null||sid.equals(""))
        	throw new Exception("sid不能为空");
       
        controlPipeline.setAutoflush(false);
        controlPipeline.markWritePosition();
        
        try
        {
            byte[] sidbt = sid.getBytes(DEFAULT_ENCODE);
            
            controlPipeline.write(IDENTIFIER_DEL);
            controlPipeline.write(sidbt.length);
            controlPipeline.write(sidbt);
            controlPipeline.flush();
            
            int idf = controlPipeline.readInt();

            if(idf!=IDENTIFIER_DEL_ACK)
                return FAILURE;

            int resp = controlPipeline.readInt();
            if(resp==NOT_MASTER)
            {
                ping();
                return del(sid);
            }
            return resp;
        }
        finally
        {
            controlPipeline.close();
            nativeCon.close();
        }
	}
	
	/**
	 * 
	 * @param sid
	 * @param quality 质量  O 高 M 中 S 低
	 * @return
	 * @throws Exception
	 */
	public StaticObject get(String sid,char quality)throws Exception
	{
	   INonBlockingConnection nativeCon = cpool.getNonBlockingConnection(masterNode);
       IMultiplexedConnection multiplexedCon = (IMultiplexedConnection) nativeCon.getAttachment();
       if(multiplexedCon==null)
       {
           multiplexedCon =  new MultiplexedConnection(nativeCon);
           nativeCon.setAttachment(multiplexedCon);
       }
        String controlPipelineId = multiplexedCon.createPipeline();   
        IBlockingPipeline controlPipeline = multiplexedCon.getBlockingPipeline(controlPipelineId); 
        controlPipeline.setConnectionTimeoutMillis(timeout);
        if(sid==null||sid.equals(""))
        	throw new Exception("sid不能为空");
        
        
        controlPipeline.setAutoflush(false);
        controlPipeline.markWritePosition();
        
        try
        {
            controlPipeline.write(IDENTIFIER_GET);
            controlPipeline.write((byte)quality);
            byte[] sidbt = sid.getBytes(DEFAULT_ENCODE);
            controlPipeline.write(sidbt.length);
            controlPipeline.write(sidbt);
            controlPipeline.flush();
                  
            controlPipeline.setAutoflush(true);
            
            int resultCode = controlPipeline.readInt();
            
            if(resultCode>0)
                return null;
            
            int idf = controlPipeline.readInt();
            if(idf!=IDENTIFIER_GET_ACK)
                return null;
            
            int dlen = controlPipeline.readInt();
            
            ByteBuffer buff = ByteBuffer.allocate(dlen);
            if(controlPipeline.read(buff)!=dlen)
                return null;
            
            StaticObject sto = new StaticObject();
            sto.unPacker(buff);
            return sto;
        }
        finally
        {
            controlPipeline.close();
            nativeCon.close();
        }
	}

	/**设置大小限制，KB **/
    public void setLimit(int limitKB)
    {
        this.limit = limitKB*1024;
    }

    public int getLimit()
    {
        return limit/1024;
    }
	
	
    public static class XThreadFactory implements ThreadFactory {
        
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean isDaemon;
        
        public XThreadFactory(boolean isDaemon) {
            this.isDaemon = isDaemon;
            namePrefix = "ClientWorkpool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(isDaemon);

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }	
	
}
