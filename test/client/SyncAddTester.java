package client;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.statics.client.StaticClient;
import org.statics.common.StaticObject;
import org.statics.common.SystemConfig;


public class SyncAddTester
{

    public static void main(String[] args) throws Exception
    {
        String sid = UUID.randomUUID().toString();
        System.out.println(sid);
        long start = System.currentTimeMillis();
        StaticClient client = new StaticClient(new String[]{"172.16.0.200:8000"},60,200);
        Map meta = new HashMap();
        meta.put("Code", "110168"); 
        
        FileInputStream fin = new FileInputStream("demo.jpg");
        int flen = fin.available();
        ByteBuffer buffer = ByteBuffer.allocateDirect(flen);
        FileChannel chl = fin.getChannel();
        chl.read(buffer);
        buffer.flip();
        byte[] data = new byte[flen];
        buffer.get(data);
        int resultCode = -1;
        try
        {
            StaticObject sto = new StaticObject(sid, "image/jpg", meta, data);
            resultCode = client.add(sto);
            System.out.println(resultCode);
        }
        catch (Exception e)
        {
           e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        
        long times = end-start;
    }
}
