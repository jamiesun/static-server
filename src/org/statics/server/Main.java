package org.statics.server;


import org.statics.common.Services;
import org.statics.service.service.ConfigService;
import org.statics.service.service.LoggerService;
import org.statics.service.service.StoreService;


public class Main {

	public static void main(String[] args) {
	    
	    //设置jmagick加载配置
	    System.setProperty("jmagick.systemclassloader","no");
	    
		//载入日志服务
		Services.put(new LoggerService());
		//载入配置服务
		Services.put(ConfigService.class);
	      //载入存储服务
        Services.put(StoreService.class);
		//载入存储路由
		Services.put(StoreHARouter.class);
		//载入TCP服务
		Services.put(StaticHandler.class);
		Services.put(TcpServer.class);
		//载入http服务
		Services.put(WebServer.class);
		//载入jmx监控服务
		Services.put(JmxServer.class);
		//载入系统监控服务
		Services.put(MonitorServer.class);
		
		//启动所有服务
		Services.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
		    public void run()
		    {
		        try
                {
		            Services.stop();
                }
                catch (Exception e)
                {
                }
		    }
		}));
	}

	public static String getString(String key,String defaultValue){
		return Services.getBean(ConfigService.class).getString(key, defaultValue);
	}
	
	public static String getString(String key){
		return Services.getBean(ConfigService.class).getString(key);
	}
	public static int getInt(String key,int defaultValue){
		return Services.getBean(ConfigService.class).getInt(key, defaultValue);
	}
	
	public static int getInt(String key){
		return Services.getBean(ConfigService.class).getInt(key);
	}

}
