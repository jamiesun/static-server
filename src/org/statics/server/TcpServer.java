package org.statics.server;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.Startable;
import org.statics.common.SystemConfig;
import org.statics.common.XThreadFactory;
import org.statics.service.service.ConfigService;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;
import org.xsocket.connection.multiplexed.MultiplexedProtocolAdapter;


public class TcpServer implements Startable,SystemConfig{

    private IServer srv;
    private final static Log log = LogFactory.getLog(TcpServer.class);
    private ConfigService configService ;
    private StaticHandler staticHandler;
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    public void setStaticHandler(StaticHandler staticHandler)
    {
        this.staticHandler = staticHandler;
    }

    public void start() {
        
        try {
            System.setProperty("org.xsocket.connection.server.readbuffer.usedirect", "true");
            srv = new Server(configService.getInt("tcp.port",8000),new MultiplexedProtocolAdapter(staticHandler));
            srv.setStartUpLogMessage("statics-tcpserver starting...");
            //srv.setWorkerpool(Executors.newCachedThreadPool(new XThreadFactory(false)));
            srv.setWorkerpool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()+1,new XThreadFactory(false)));
            srv.setConnectionTimeoutMillis(10*60*1000);
            srv.setIdleTimeoutMillis(60*1000);
            srv.setFlushmode(FlushMode.ASYNC);
            srv.start();
            ConnectionUtils.registerMBean(srv);
        }
        catch (UnknownHostException e) 
        {
            log.error("无效的主机标识", e);
        } 
        catch (Exception e) 
        {
            log.error("TCP服务启动失败",e);
        }

    }

    public void stop() {
        try
        {
            srv.close();
            srv = null;
        } catch (IOException e) {
            log.error("TCP服务关闭时错误",e);
        }
        log.info("TcpServer has been shutdown");
    }




}
