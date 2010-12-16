package client;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.statics.client.ClientHandler;
import org.statics.client.StaticClient;
import org.statics.common.StaticObject;



public class AnsycAddtester
{

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        String sid = UUID.randomUUID().toString();
        System.out.println(sid);
        long start = System.currentTimeMillis();
        StaticClient client = new StaticClient(new String[]{"172.16.0.200:8000","127.0.0.1:8000","127.0.0.1:8002"},20,5);
        client.setLimit(1000);
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
        StaticObject sto = new StaticObject(sid, "jmage/jpg", meta, data);
        client.add(sto, new ClientHandler(){
            public void onResponse(int status)
            {
                System.out.println(status);
                
            }

        });
        
        Thread.sleep(5000);

    }

}
