package com.robert.maps.applib.utils;

import java.util.concurrent.ThreadFactory;

public class SimpleThreadFactory implements ThreadFactory {
	private final String mThreadName; 

	public SimpleThreadFactory(final String threadName) {
		super();
		mThreadName = threadName;
	}

	public Thread newThread(Runnable r) {
		return new Thread(r, mThreadName);
	}
	
}
