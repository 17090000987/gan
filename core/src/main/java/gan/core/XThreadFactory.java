package gan.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class XThreadFactory implements ThreadFactory{
	final AtomicInteger threadNumber = new AtomicInteger(1);
	
	final String		name;
	
	public XThreadFactory(String name) {
		this.name = name;
	}
	
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(null, r, 
				new StringBuilder().append(name).append("-")
				.append(threadNumber.getAndIncrement()).toString(), 0);
		int priority = Thread.NORM_PRIORITY - 2;
		if(priority < Thread.MIN_PRIORITY){
			priority = Thread.MIN_PRIORITY;
		}
		t.setPriority(priority);
		return t;
	}

}
