package org.statics.service.service;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.Startable;
import org.statics.common.Utils;
import org.statics.server.store.StaticAccessor;

import com.sleepycat.je.Durability;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.RepInternal;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.TimeConsistencyPolicy;
import com.sleepycat.je.rep.jmx.RepJEMonitor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class StoreService implements Startable{

    private static final int NODE_PORT_DISPLACEMENT = 100;
    private ReplicatedEnvironment repEnv ;
	private EntityStore store;
	private EnvironmentConfig envConfig;
	private ReplicationConfig repCfg;
	private StaticAccessor staticAccess;
    private ConfigService configService ;
    private final static Log log = LogFactory.getLog(StoreService.class);
    public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}
    public void start() {
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        
        File envHome = new File(configService.getString("store.path","statics"));
        if(!envHome.exists())
            envHome.mkdirs();
        try
        {
            repCfg = new ReplicationConfig();
            
            /**
             * 存储服务同步端口 = 服务器端口（tcp.port） + 偏移值（NODE_PORT_DISPLACEMENT）
             */
            int nodePort = configService.getInt("tcp.port",8000)+NODE_PORT_DISPLACEMENT;
            String nodeAddress = Utils.getLocalIP()+":"+nodePort;
            String heplerAddress = configService.getString("node.helpers",nodeAddress);
            
            repCfg.setNodeName(configService.getString("node.name","statics0"));
            repCfg.setGroupName(configService.getString("node.group","staticGroup"));
            repCfg.setNodeHostPort(nodeAddress);
            repCfg.setHelperHosts(heplerAddress);
            TimeConsistencyPolicy consistencyPolicy = new TimeConsistencyPolicy
            (1, TimeUnit.SECONDS, /* 1 sec of lag */
             3, TimeUnit.SECONDS  /* Wait up to 3 sec */);
            repCfg.setConsistencyPolicy(consistencyPolicy);
            repCfg.setReplicaAckTimeout(2, TimeUnit.SECONDS);   
            repCfg.setPriority(0);
            log.info("StoreService node -name "
                + repCfg.getNodeName() + " -group " + repCfg.getGroupName()
                + " -nodeHost " + repCfg.getNodeHostPort() + " - helperHosts "
                + repCfg.getHelperHosts());
            
            envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            envConfig.setTransactional(true);
            envConfig.setCacheSize(1024*1024*256);
            Durability durability =
                new Durability(Durability.SyncPolicy.WRITE_NO_SYNC,
                               Durability.SyncPolicy.WRITE_NO_SYNC,
                               Durability.ReplicaAckPolicy.NONE);
            envConfig.setDurability(durability);
            envConfig.setConfigParam("je.log.fileMax",String.valueOf(1024*1024*256));
            RepInternal.setAllowConvert(repCfg, true);
            repEnv = new ReplicatedEnvironment(envHome,repCfg,envConfig);
            store = new EntityStore(repEnv, "StaticStore", storeConfig);
            RepJEMonitor monitor = new RepJEMonitor();
            monitor.doRegister(repEnv);

        }
        catch (Exception e)
        {
           log.error("StoreService starting error", e);
        }

        staticAccess = new StaticAccessor(store);
	}

	public void stop() {

	    log.info("StoreService ready shutdown,sync data....");
	    store.sync();
		store.close();
		repEnv.close();
		log.info("StoreService has been shutdown");
		
	}

	
	public StaticAccessor getStaticAccess() {
		return staticAccess;
	}

	
	public Transaction  beginTransaction(Transaction tn,TransactionConfig cf)
	{
		return repEnv.beginTransaction(tn, cf);
	}

	public Transaction  beginTransaction()
	{
		return repEnv.beginTransaction(null,null);
	}



}


