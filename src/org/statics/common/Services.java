package org.statics.common;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.CachingComponentAdapterFactory;
import org.picocontainer.defaults.DefaultPicoContainer;
import org.picocontainer.defaults.SetterInjectionComponentAdapterFactory;


/**
 * 实例管理器,系统初始化时将所有服务组件装入
 * @author wangjuntao
 * 
 */
public class Services
{
    private final MutablePicoContainer pico = new DefaultPicoContainer(new CachingComponentAdapterFactory(new SetterInjectionComponentAdapterFactory()));
    private final static Services beans = new Services();
    private final static Log log = LogFactory.getLog(Services.class);

    private Services()
    {
    }

    public static Services getInstance()
    {
        return beans;
    }

    @SuppressWarnings("unchecked")
	public void registryBean(Class clasz)
    {
        pico.registerComponentImplementation(clasz);
        log.info("registry [" + clasz.getName() + "] done !");
    }

    public void registryBean(Object obj)
    {
        pico.registerComponentInstance(obj);
        log.info("registry [" + obj.getClass().getName() + "]  done !");
    }

    public void startAll()
    {
        pico.start();
    }
    
    public void stopAll()
    {
        pico.stop();
    }
    
    public void dispose()
    {
    	pico.dispose();
    }

    @SuppressWarnings("unchecked")
	public <T> T get(Class<T> clasz)
    {
        return (T) pico.getComponentInstanceOfType(clasz);
    }
    
	public static <T> T getBean(Class<T> clasz)
    {
        return getInstance().get(clasz);
    }

    @SuppressWarnings("unchecked")
	public static void put(Class cs)
    {
        getInstance().registryBean(cs);
    }

    /**
     * 置入一个服务组件
     * @param obj
     */
    public static void put(Object obj)
    {
        getInstance().registryBean(obj);
    }

    /**
     * 启动所有实现startable的实例
     */
    public static void start()
    {
        getInstance().startAll();
    }
    
    public static void stop()
    {
        getInstance().stopAll();
    }
    

    public String toString()
    {
        return pico.toString();
    }
    


}
