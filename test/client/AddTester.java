package client;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.statics.client.StaticClient;
import org.statics.common.StaticObject;
import org.statics.common.SystemConfig;




public class AddTester implements SystemConfig
{

    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        
        int c = Integer.valueOf(args[0]);
        int n = Integer.valueOf(args[1]);
        ExecutorService execPool =   Executors.newFixedThreadPool(c);
        FileInputStream fin = new FileInputStream("demo.jpg");
        int flen = fin.available();
        ByteBuffer buffer = ByteBuffer.allocateDirect(flen);
        FileChannel chl = fin.getChannel();
        chl.read(buffer);
        buffer.flip();
        byte[] data = new byte[flen];
        buffer.get(data);
        StaticClient client = new StaticClient(new String[]{"172.16.0.254:8002","172.16.0.200:8000"},60,200);
        client.setLimit(1000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++)
        {
            execPool.submit(new call(client,data));
        }
        execPool.shutdown();
        
        while(!execPool.isTerminated())
        {
            Thread.sleep(1000);
        }
        long end = System.currentTimeMillis();
        
        long times = end-start;
        
        long per = n/(times/1000);
        
        long pertime = times/n;
        
        System.out.println("测试并发总数："+c);
        System.out.println("测试消息总数："+n);
        System.out.println("测试耗时："+times+"ms");
        System.out.println("每秒处理请求数："+per);
        System.out.println("平均每个请求耗时："+pertime);


    }


}

class call implements Runnable{
    private byte[] data;
    StaticClient client;
    public call(StaticClient client,byte[] data)
    {
        this.data = data;
        this.client = client;
    }

    public void run()
    {
        String sid = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        
        Map meta = new HashMap();
        meta.put("Code", "110168"); 
        int resultCode = -1;
        try
        {
            StaticObject sto = new StaticObject(sid, "image/jpg", meta, data);
            resultCode = client.add(sto);
        }
        catch (Exception e)
        {
           e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        
        long times = end-start;
        
        //System.out.println(Thread.currentThread()+"::"+"senddata " +sid+ " resultCode:"+resultCode +" cast："+times+"ms");
        
    }
    
}


