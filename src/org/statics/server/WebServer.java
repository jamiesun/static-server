package org.statics.server;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.Startable;
import org.statics.common.XThreadFactory;
import org.statics.service.service.ConfigService;
import org.xlightweb.Context;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.ConnectionUtils;


public class WebServer implements Startable {

    private HttpServer hsrv ;
    private final static Log log = LogFactory.getLog(WebServer.class);
    private ConfigService configService ;
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    public void start() {
        Context rootCtx = new Context("");
        rootCtx.addHandler(new HttpHandler());
        System.setProperty("org.xlightweb.showDetailedError", "true");
        try {
            hsrv = new HttpServer(configService.getInt("http.port",9000),rootCtx);
            hsrv.setMaxConcurrentConnections(configService.getInt("http.max",1024));
            hsrv.setWorkerpool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),new XThreadFactory(false)));
            hsrv.setStartUpLogMessage("statics-webserver starting...");
            hsrv.start();
            ConnectionUtils.registerMBean(hsrv);
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
        hsrv.close();
        hsrv = null;
        log.info("WebServer has been shutdown");
    }

}
