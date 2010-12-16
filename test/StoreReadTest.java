import org.statics.server.store.StaticAccessor;
import org.statics.server.store.StaticData;
import org.statics.server.store.StaticFile;
import org.statics.service.service.ConfigService;
import org.statics.service.service.StoreService;

import com.sleepycat.persist.EntityCursor;


public class StoreReadTest {

	public static void main(String[] args) {
		StoreService serv = new StoreService();
		ConfigService cf = new ConfigService();
		serv.setConfigService(cf);
		serv.start();
		final StaticAccessor dao = serv.getStaticAccess();
		
		EntityCursor<StaticFile> cur = dao.getStaticFileBySid().entities();
		long start = System.currentTimeMillis();
		for (StaticFile sf : cur) {
			System.out.println(sf);
			System.out.println(dao.getStaticData(sf.getSmall()).getData().length);
			System.out.println(System.currentTimeMillis()-start);
			break;
		}
		System.out.println(dao.getStaticDataById().count());
		
		
	}
}
