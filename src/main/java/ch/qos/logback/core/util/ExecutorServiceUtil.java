/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static utility methods for manipulating an {@link ExecutorService}.
 * 
 * @author Carl Harris
 * @author Mikhail Mazursky
 */
public class ExecutorServiceUtil {

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread thread = defaultFactory.newThread(r);
            if (!thread.isDaemon()) {
                thread.setDaemon(true);
            }
            thread.setName("logback-" + threadNumber.getAndIncrement());
            return thread;
        }
    };
    
    public static final LogbackScheduler scheduler = new LogbackScheduler(1, THREAD_FACTORY);

    public static ScheduledExecutorService newScheduledExecutorService() {
        return scheduler;
    }

    /**
     * Creates an executor service suitable for use by logback components.
     * @return executor service
     */
    public static ExecutorService newExecutorService() {
        return scheduler.getExecutorService();
    }

    /**
     * Shuts down an executor service.
     * <p>
     * @param executorService the executor service to shut down
     */
    public static void shutdown(ExecutorService executorService) {
        executorService.shutdownNow();
    }

    /**
     * ScheduledThreadPoolExecutor不会按需创建新的线程，logback调用submit、execute时可能会长期占用线程，导致缺少线程执行scheduleWithFixedDelay等定时任务，
     * 因此LogbackScheduler创建额外的线程池来执行此类耗时任务
     */
    public static class LogbackScheduler extends ScheduledThreadPoolExecutor {
    	ExecutorService es = null;
    	public LogbackScheduler(int corePoolSize, ThreadFactory threadFactory) {
    		super(corePoolSize, threadFactory);
    		es = Executors.newCachedThreadPool(threadFactory);
    	}
    	public ExecutorService getExecutorService() {
    		return es;
    	}
    	@Override
    	public void execute(Runnable command) {
    		es.execute(command);
    	}
    	@Override
    	public Future<?> submit(Runnable task) {
    		return es.submit(task);
    	}
    	@Override
    	public <T> Future<T> submit(Runnable task, T result) {
    		return es.submit(task, result);
    	}
    	@Override
    	public <T> Future<T> submit(Callable<T> task) {
    		return es.submit(task);
    	}
    	@Override
    	public void shutdown() {
    		super.shutdown();
    		es.shutdown();
    	}
    	@Override
    	public List<Runnable> shutdownNow() {
    		es.shutdownNow();
    		return super.shutdownNow();
    	}
    }

}
