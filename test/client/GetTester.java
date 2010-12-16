package client;

import org.statics.client.StaticClient;
import org.statics.common.StaticObject;


public class GetTester
{

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        String sid = "e21af408-6dbe-4fa7-80fc-414e4e86b285";
        long start = System.currentTimeMillis();
        StaticClient client = new StaticClient(new String[]{"127.0.0.1:8000","127.0.0.1:8001"},20,200);
        StaticObject obj = client.get(sid, StaticClient.QUALITY_SMALL);

        if (obj != null)
        {
            System.out.println(obj.getSid());
            System.out.println(obj.getType());
            System.out.println(obj.getMeta());
            System.out.println(obj.getData().length);
        }
        long end = System.currentTimeMillis();

        long times = end - start;

        System.out.println(Thread.currentThread() + "::" + "senddata " + sid
            + " cast：" + times + "ms");

    }

}
