package org.statics.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.Startable;
import org.statics.common.Utils;
import org.statics.service.service.ConfigService;


public class JmxServer implements Startable
{
    private static final int RMI_PORT_DISPLACEMENT = 10000;
    private static final int JMX_PORT_DISPLACEMENT = 11000;
    
    private JMXConnectorServer jmxConnServer;
    
    private ConfigService configService ;
    private final static Log log = LogFactory.getLog(JmxServer.class);
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    public void start()
    {
        try
        {
            int tcpPort = configService.getInt("tcp.port",8000);
            int rmiport = RMI_PORT_DISPLACEMENT+tcpPort;
            int jmxPort = JMX_PORT_DISPLACEMENT+tcpPort;
            String host = Utils.getLocalIP();
            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            LocateRegistry.createRegistry(rmiport);
            String jmxUrl = "service:jmx:rmi://"+host+":"+jmxPort+"/jndi/rmi://"+host+":"+rmiport+"/server";
            log.info("JMXServiceURL "+jmxUrl);
            JMXServiceURL url = new JMXServiceURL(jmxUrl);
            jmxConnServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbserver);
            jmxConnServer.start();   
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
     
        
    }

    public void stop()
    {

        try
        {
            jmxConnServer.stop();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
