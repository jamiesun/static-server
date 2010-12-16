package org.statics.common;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.statics.service.service.ConfigService;


public class Shutdown
{
    /** Shutdown入口 */
    public static void main(String[] args)
    {
        new Shutdown().stop();
    }
    
    /** 起始Socket并发送关闭指令 */
    private void stop()
    {
        try
        {
            ConfigService cfg = new ConfigService();
            int port = cfg.getInt("tcp.port",8000)+cfg.getInt("http.port",9000);
            if (port < 1 || port > 65535)
                return;
            
            Socket s = new Socket(InetAddress.getByName("127.0.0.1"), port);
            OutputStream out=s.getOutputStream();
            out.write(("statics\r\nstop\r\n").getBytes());
            out.flush();
            s.shutdownOutput();
            s.close();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
