package org.statics.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.Startable;
import org.statics.common.Services;
import org.statics.server.store.StaticFile;
import org.statics.service.service.ConfigService;
import org.statics.service.service.StoreService;


public class MonitorServer implements Startable
{
    private static final Log logger = LogFactory.getLog(MonitorServer.class);
    private ServerSocket serverSocket;
    private ConfigService configService ;
    private Thread mthread ;
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
    public void start()
    {
        int port = configService.getInt("tcp.port",8000)+configService.getInt("http.port",9000);
        try
        {
            serverSocket = new ServerSocket(port,1,InetAddress.getByName("127.0.0.1"));
            mthread = new Thread(new Handler(),"MonitorServer:"+port);
            mthread.setDaemon(true);
            mthread.start();
        }
        catch (UnknownHostException e)
        {
            logger.error("启动监视线程失败", e);
        }
        catch (IOException e)
        {
            logger.error("启动监视线程失败", e);
        }
        catch (Exception e)
        {
            logger.error("启动监视线程失败", e);
        }
        
    }

    public void stop()
    {
        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
        }
    }
    
    class Handler implements Runnable{

        public void run()
        {
            while (true)
            {
                Socket socket = null;
                try
                {
                    socket = serverSocket.accept();
                    LineNumberReader line = new LineNumberReader(new InputStreamReader(socket.getInputStream()));
                    String lineKey = line.readLine();
                    if ("statics".equals(lineKey))
                    {
                        String cmd= line.readLine();
                        if ("stop".equals(cmd))
                        {
                            try {socket.close();}catch(Exception e){logger.error(e);}
                            try {serverSocket.close();}catch(Exception e){logger.error(e);}
                            Services.stop();
                            System.exit(0);
                        }
                        else
                        {
                            String s = "Command is not supported";
                            OutputStream out=socket.getOutputStream();
                            out.write(s.getBytes());
                            out.flush();
                        }
                    }
                    else if ("StoreShell".equals(lineKey))
                    {
                        String cmd= line.readLine();
                        if("status".equals(cmd))
                        {
                            StringBuffer buff = new StringBuffer();
                            Map<String , InetSocketAddress> ndm = Services.getBean(StoreHARouter.class).getNodeAddressMapping();
                            buff.append("NodeAddressMap:\r\n");
                            for (Map.Entry<String , InetSocketAddress> ent: ndm.entrySet())
                            {
                                buff.append(ent.getKey()).append("=>").append(ent.getValue()).append("\r\n");
                            }
                            buff.append("\r\n");
                            List<InetSocketAddress> asa = Services.getBean(StoreHARouter.class).getActiveServerAddresses();
                            buff.append("ActiveServerAddresses:\r\n");
                            for (InetSocketAddress sa : asa)
                            {
                                buff.append(sa).append("\r\n");
                            }
                            buff.append("\r\n");
                            String master = Services.getBean(StoreHARouter.class).getMasterName();
                            InetSocketAddress maddr = Services.getBean(StoreHARouter.class).getNodeMasterAddress();
                            buff.append("master:").append(master).append("=>").append(maddr).append("\r\n\r\n");
                            OutputStream out=socket.getOutputStream();
                            out.write(buff.toString().getBytes());
                            out.flush();
                        }
                        else if("size".equals(cmd))
                        {
                            long size = Services.getBean(StoreService.class).getStaticAccess().size();
                            OutputStream out=socket.getOutputStream();
                            out.write((""+size).getBytes());
                            out.flush();
                        }
                        else if(cmd.startsWith("get"))
                        {
                            String sid = cmd.substring(3).trim();
                            StaticFile sf = Services.getBean(StoreService.class).getStaticAccess().getStaticFile(sid);
                            String sfstr = sf==null?"None":sf.toString();
                            OutputStream out=socket.getOutputStream();
                            out.write((sfstr).getBytes());
                            out.flush();
                        }
                        else if(cmd.startsWith("del"))
                        {
                            String sid = cmd.substring(3).trim();
                            boolean yn  = Services.getBean(StoreService.class).getStaticAccess().delete(sid);
                            OutputStream out=socket.getOutputStream();
                            out.write((String.valueOf(yn)).getBytes());
                            out.flush();
                        }
                        else if(cmd.startsWith("list"))
                        {
                            String no = cmd.substring(4).trim();
                            int ino = 10;
                            try
                            {
                                ino = Integer.parseInt(no);
                            }
                            catch (Exception e)
                            {
                                ino = 10;
                            }
                            StringBuffer buff = new StringBuffer();
                            List<StaticFile> flist = Services.getBean(StoreService.class).getStaticAccess().listFiles(ino);
                            for (StaticFile staticFile : flist)
                            {
                                buff.append(staticFile.toString()).append("\r\n");
                            }
                            String sfstr = buff.length()==0?"None":buff.toString();
                            OutputStream out=socket.getOutputStream();
                            out.write(sfstr.getBytes());
                            out.flush();
                        }
                        else
                        {
                            String s = "Command is not supported";
                            OutputStream out=socket.getOutputStream();
                            out.write(s.getBytes());
                            out.flush();
                        }
                    }

                }
                catch(Exception e)
                {
                    logger.error(e);
                }
                finally
                {
                    try
                    {
                        socket.close();
                    }
                    catch (IOException e)
                    {
                        logger.error(e);
                    }
                }
            }
            
        }
        
    }
}
