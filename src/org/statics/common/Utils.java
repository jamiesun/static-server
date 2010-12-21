package org.statics.common;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.Enumeration;

public class Utils {

    public final static String md5Encoder(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes();
            // 使用MD5创建MessageDigest对象
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte b = md[i];
                str[k++] = hexDigits[b >> 4 & 0xf];
                str[k++] = hexDigits[b & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }
    
    /** 
     * ip地址转成整数. 
     * @param ip 
     * @return 
     */  
    public static long ip2long(String ip) {  
        String[] ips = ip.split("[.]");  
        long num =  16777216L*Long.parseLong(ips[0]) + 65536L*Long.parseLong(ips[1]) + 256*Long.parseLong(ips[2]) + Long.parseLong(ips[3]);  
        return num;  
    }  
      
    /** 
     * 整数转成ip地址. 
     * @param ipLong 
     * @return 
     */  
    public static String long2ip(long ipLong) {  
        //long ipLong = 1037591503;  
        long mask[] = {0x000000FF,0x0000FF00,0x00FF0000,0xFF000000};  
        long num = 0;  
        StringBuffer ipInfo = new StringBuffer();  
        for(int i=0;i<4;i++){  
            num = (ipLong & mask[i])>>(i*8);  
            if(i>0) ipInfo.insert(0,".");  
            ipInfo.insert(0,Long.toString(num,10));  
        }  
        return ipInfo.toString();  
    }  
    
    public static InetAddress getLocalAddress()
    {
        InetAddress addr = null;
        try
        {
            Enumeration netInterfaces=NetworkInterface.getNetworkInterfaces();  
            InetAddress ip = null;  
            while(netInterfaces.hasMoreElements())  
            {  
                NetworkInterface ni=(NetworkInterface)netInterfaces.nextElement();    
                ip=(InetAddress) ni.getInetAddresses().nextElement();  
                if( !ip.isLoopbackAddress())  
                {  
                    addr =  ip;
                    break;
                }  
            }
            addr = addr!=null?addr:InetAddress.getLocalHost();
        }
        catch (Exception e)
        {
        }
        return addr;

    }
    
    public static String getLocalIP() {
        String ip = "";
        try {
            Enumeration<?> e1 = (Enumeration<?>) NetworkInterface
                    .getNetworkInterfaces();
            while (e1.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e1.nextElement();
                if (!ni.getName().equals("eth0")) {
                    continue;
                } else {
                    Enumeration<?> e2 = ni.getInetAddresses();
                    while (e2.hasMoreElements()) {
                        InetAddress ia = (InetAddress) e2.nextElement();
                        if (ia instanceof Inet6Address)
                            continue;
                        ip = ia.getHostAddress();
                    }
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return ip;
    }
    public static void main(String[] args)
    {
        System.out.println(getLocalIP());
    }

}
