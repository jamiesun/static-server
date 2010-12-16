import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.statics.server.store.StaticAccessor;
import org.statics.server.store.StaticData;
import org.statics.server.store.StaticFile;
import org.statics.service.service.ConfigService;
import org.statics.service.service.StoreService;



public class StoreTest {

	public static void main(String[] args) throws Exception {
		ExecutorService execPool =   Executors.newFixedThreadPool(2);
		StoreService serv = new StoreService();
		ConfigService cf = new ConfigService();
		serv.setConfigService(cf);
		serv.start();
		final StaticAccessor dao = serv.getStaticAccess();
		
        FileInputStream fin = new FileInputStream("demo.jpg");
        int flen = fin.available();
        ByteBuffer buffer = ByteBuffer.allocateDirect(flen);
        FileChannel chl = fin.getChannel();
        chl.read(buffer);
        buffer.flip();
        byte[] data = new byte[flen];
        
        int n = 1000;
        long start = System.currentTimeMillis();
        
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("code", "110168");
        
        for (int i = 0; i < n; i++) {
    		UUID uuid = UUID.randomUUID();

    		final StaticFile sf = new StaticFile(uuid.toString(),"image/jpg",meta,null,null,null);
    		final StaticData sd = new StaticData(uuid.toString(),data);
    		
    		sf.setOriginal(uuid.toString());
    		execPool.submit(new Runnable(){
				public void run() {
					dao.add(sf, sd);
					
				}
    			
    		});
		}
        execPool.shutdown();
        while(!execPool.isTerminated())
        {
            Thread.sleep(1000);
        }
		
        long total = System.currentTimeMillis()-start-1000;
        
        long per = n/(total/1000);
        
        long pertime = total/n;
        
        System.out.println("共处理请求:"+n);
        System.out.println("共耗时(毫秒:"+total);
        System.out.println("每秒处理:"+per);
        System.out.println("平均每次耗时:"+pertime);

//		System.out.println(dao.getStaticDataById().get(uuid.toString()));
//		System.out.println(dao.getStaticFileBySid().get(uuid.toString()));
		
	}
	

}
