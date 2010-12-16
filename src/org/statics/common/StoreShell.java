package org.statics.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.statics.service.service.ConfigService;


public class StoreShell
{
    public static void main(String[] args)
    {
        ConfigService cfg = new ConfigService();
        int port = cfg.getInt("tcp.port",8000)+cfg.getInt("http.port",9000);
        if (port < 1 || port > 65535)
            return;
        try
        {
            
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                System.out.print("StoreShell>");
                String cmd = stdin.readLine();
                if("quit".equals(cmd))
                {
                    System.exit(0);
                }
                if(cmd.length()<64)
                {
                    Socket s = new Socket(InetAddress.getByName("127.0.0.1"), port);
                    OutputStream out=s.getOutputStream();
                    out.write(("StoreShell\r\n"+cmd+"\r\n").getBytes());
                    out.flush();
                    BufferedReader sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String line = null;
                    while((line=sin.readLine())!=null)
                    {
                        System.out.println(line);
                    }
                    s.close();
                }
            }
            
        }
        catch (UnknownHostException e)
        {
            System.err.println("无效主机127.0.0.1："+port);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }

    }
    


}
