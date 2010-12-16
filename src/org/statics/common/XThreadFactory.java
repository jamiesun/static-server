package org.statics.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XThreadFactory implements ThreadFactory {
    
	private final static Log log = LogFactory.getLog(XThreadFactory.class);
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean isDaemon;

    public XThreadFactory(boolean isDaemon) {
        this.isDaemon = isDaemon;
        namePrefix = "ServerWorkpool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread(Runnable r) {
    	String name = namePrefix + threadNumber.getAndIncrement();
        log.info("creating new thread:"+name);
        Thread t = new Thread(r,name );
        t.setDaemon(isDaemon);

        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}