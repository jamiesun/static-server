package client;

import org.statics.client.StaticClient;

public class DelTester
{

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        String sid = "d720823d-3f77-4374-9ffe-ec5532efe730";
        long start = System.currentTimeMillis();
        StaticClient client = new StaticClient(new String[]{"127.0.0.1:8000","127.0.0.1:8001"},20,200);
        client.del(sid);
        long end = System.currentTimeMillis();
        
        long times = end-start;
        
        System.out.println(Thread.currentThread()+"::"+"senddata "+ sid +" cast："+times+"ms");
    }

}
