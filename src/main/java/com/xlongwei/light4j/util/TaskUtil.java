package com.xlongwei.light4j.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * 执行单次任务或定时任务工具类（用于减少new Thread()和new Timer()的使用）
 * @author hongwei
 */
@Slf4j
public class TaskUtil {
	private static ExecutorService cachedExecutor = null;
	private static ScheduledExecutorService scheduledExecutor = null;
	private static Map<Runnable, Future<?>> keepRunningTasks = null;
	private static Map<String, Future<?>> cancelTrackingTasks = new HashMap<>();
	private static Map<Future<?>, Callback> callbackdTasks = null;
	private static List<Object> shutdownHooks = new LinkedList<>();
	static {
		cachedExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new TaskUtilThreadFactory("cached"));
		scheduledExecutor = new ScheduledThreadPoolExecutor(1, new TaskUtilThreadFactory("scheduled"));
		Runtime.getRuntime().addShutdownHook(new Thread() { @Override public void run() { TaskUtil.shutdown(); } });
	}
	
	/**
	 * 关闭TaskUtil，通常情况下不必手动调用
	 */
	public static void shutdown() {
		if(cancelTrackingTasks!=null && cancelTrackingTasks.size()>0) {
			for(String task : cancelTrackingTasks.keySet()) {
				Future<?> future = cancelTrackingTasks.get(task);
				cancel(future);
			}
		}
		if(shutdownHooks!=null && shutdownHooks.size()>0) {
			for(Object shutdownHook:shutdownHooks) {
				log.info("shutdown: "+shutdownHook);
				Class<? extends Object> clazz = shutdownHook.getClass();
				if(Closeable.class.isAssignableFrom(clazz)) {
					try {
						((Closeable)shutdownHook).close();
					}catch(IOException e) {
						log.warn("fail to shutdown Closeable: "+shutdownHook, e);
					}
				}else if(Runnable.class.isAssignableFrom(clazz)) {
					TaskUtil.submit((Runnable)shutdownHook);
				}else if(Callable.class.isAssignableFrom(clazz)) {
					TaskUtil.submit((Callable<?>)shutdownHook);
				}else if(Thread.class.isAssignableFrom(clazz)) {
					((Thread)shutdownHook).start();
				}
			}
		}
		scheduledExecutor.shutdown();
		cachedExecutor.shutdown();
		log.info("TaskUtil executors shutdown.");
		if(!scheduledExecutor.isTerminated()) {
			scheduledExecutor.shutdownNow();
		}
		if(!cachedExecutor.isTerminated()) {
			scheduledExecutor.shutdownNow();
		}
	}
	
	/**
	 * @param shutdownHook
	 * <ul>
	 * <li>Closeable
	 * <li>Runable or Callable
	 * <li>Thread
	 */
	public static boolean addShutdownHook(Object shutdownHook) {
		Class<? extends Object> clazz = shutdownHook.getClass();
		boolean validShutdownHook = false;
		if(Closeable.class.isAssignableFrom(clazz)) {
			validShutdownHook = true;
		}
		boolean isRunnableOrCallable = !validShutdownHook && (Runnable.class.isAssignableFrom(clazz) || Callable.class.isAssignableFrom(clazz));
		if(isRunnableOrCallable) {
			validShutdownHook = true;
		}
		if(!validShutdownHook && Thread.class.isAssignableFrom(clazz)) {
			validShutdownHook = true;
		}
		if(validShutdownHook) {
			shutdownHooks.add(shutdownHook);
		}
		return validShutdownHook;
	}

	/**
	 * 立即执行任务
	 */
	public static Future<?> submit(Runnable task) {
		Future<?> future = cachedExecutor.submit(task);
		return future;
	}
	
	/**
	 * 自动保持任务持续运行，每分钟监视一次
	 */
	public static Future<?> submitKeepRunning(Runnable task){
		Future<?> future = submit(task);
		cancelTrackingTasks.put(task.toString(), future);
		checkInitCachedTasks();
		synchronized (keepRunningTasks) {
			keepRunningTasks.put(task, future);
		}
		return future;
	}
	
	/**
	 * 延迟执行任务，例如延迟5秒：schedule(task,5,TimeUnit.SECONDS)
	 */
	public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		ScheduledFuture<?> future = scheduledExecutor.schedule(new ScheduleTask(task), delay, unit);
		return future;
	}
	
	/**
	 * 定时执行任务一次，比如下午两点：scheduleAt(task, DateUtils.setHours(new Date(), 13))
	 */
	public static ScheduledFuture<?> scheduleAt(Runnable task, Date time) {
		long mills = time.getTime() - System.currentTimeMillis();
		if(mills > 0) {
			return schedule(task, mills, TimeUnit.MILLISECONDS);
		} else {
			log.info("scheduleAt "+time+" not executed cause of passed "+new Date());
		}
		return null;
	}
	
	/**
	 * 定时重复执行任务，比如延迟5秒，每10分钟执行一次：scheduleAtFixRate(task, 5, TimeUnit.MINUTES.toSeconds(10), TimeUnit.SECONDS)
	 */
	public static void scheduleAtFixedRate(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		ScheduledFuture<?> future = scheduledExecutor.scheduleWithFixedDelay(new ScheduleTask(task), initialDelay, delay, unit);
		cancelTrackingTasks.put(task.toString(), future);
	}
	
	/**
	 * 定时重复执行任务，比如下午两点开始，每小时执行一次：scheduleAtFixRate(task, DateUtils.setHours(new Date(), 13), 1, TimeUnit.HOURS)
	 */
	public static void scheduleAtFixedRate(Runnable task, Date time, long delay, TimeUnit unit) {
		long mills = time.getTime() - System.currentTimeMillis();
		if(mills <= 0) {
			long span = unit.toMillis(delay);
			long at = time.getTime();
			long current = System.currentTimeMillis();
			while(at <= current)
			 {
				//寻找下次合适执行时机，按天执行的保留小时准确，按小时执行的保留分钟准确
				at += span; 
			}
			mills = at - current;
		}
		scheduleAtFixedRate(task, mills, unit.toMillis(delay), TimeUnit.MILLISECONDS);
	}
	
	public static void cancel(Runnable task) {
		boolean cancel = cancel(cancelTrackingTasks.get(task.toString()));
		if(cancel) {
			cancelTrackingTasks.remove(task.toString());
		} 
		keepRunningTasks.remove(task);
	}
	
	public static void cancel(Callable<?> task) {
		boolean cancel = cancel(cancelTrackingTasks.get(task.toString()));
		if(cancel) {
			cancelTrackingTasks.remove(task.toString());
		}
	}
	
	public static boolean cancel(Future<?> future) {
		if(future==null) {
			return false;
		}
		if(!future.isDone() && !future.isCancelled()) {
			return future.cancel(true);
		}
		return true;
	}
	
	/**
	 * 提交带返回值的任务，支持后续处理
	 */
	public static <T> Future<T> submit(Callable<T> task) {
		Future<T> future = cachedExecutor.submit(task);
		return future;
	}
	
	/**
	 * 提交带返回值的任务，支持后续处理
	 */
	public static <T> Future<T> submit(Callable<T> task, Callback callback) {
		Future<T> future = submit(task);
		checkInitCachedTasks();
		if(callback != null) {
			synchronized (callbackdTasks) {
				callbackdTasks.put(future, callback);
			}
		}
		return future;
	}
	
	/**
	 * 提交任务，等待返回值
	 */
	public static <T> T wait(Callable<T> task) {
		Future<T> future = cachedExecutor.submit(task);
		try {
			return future.get();
		} catch (Exception e) {
			log.warn("fail to wait task: "+task, e);
			future.cancel(true);
			return null;
		}
	}
	
	public static void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}catch(Exception e) {
			log.warn("fail to sleep millis: "+milliseconds, e);
		}
	}
	
	private static void checkInitCachedTasks() {
		if(keepRunningTasks != null) {
			return;
		}
		keepRunningTasks = new HashMap<Runnable, Future<?>>(16);
		callbackdTasks = new HashMap<Future<?>, Callback>(16);
		scheduleAtFixedRate(new CachedTasksMonitor(), 1, 1, TimeUnit.MINUTES);
	}
	
	/**
	 * 监视需要保持运行的任务
	 */
	private static class CachedTasksMonitor implements Runnable {
		@Override
		public void run() {
			if(keepRunningTasks.size() > 0) {
				synchronized (keepRunningTasks) {
					Map<Runnable, Future<?>> tempTasks = null;
					for(Runnable task : keepRunningTasks.keySet()) {
						Future<?> future = keepRunningTasks.get(task);
						if(future.isDone()) {
							//恢复运行结束任务
							future = submit(task);
							if(tempTasks == null) {
								tempTasks = new HashMap<Runnable, Future<?>>(4);
							}
							tempTasks.put(task, future);
							cancelTrackingTasks.put(task.toString(), future);
						}
					}
					if(tempTasks != null && tempTasks.size() > 0) {
						keepRunningTasks.putAll(tempTasks);
					}
				}
			}
			
			if(callbackdTasks.size() > 0) {
				synchronized (callbackdTasks) {
					List<Future<?>> callbackedFutures = null;
					for(Map.Entry<Future<?>, Callback> entry : callbackdTasks.entrySet()) {
						Future<?> future = entry.getKey();
						Callback callback = entry.getValue();
						if(future.isDone()) {
							try{
								final Object result = future.get(5, TimeUnit.SECONDS);
								submit(() -> {
										callback.handle(result);
								});
								if(callbackedFutures == null) {
									callbackedFutures = new LinkedList<Future<?>>();
								}
								callbackedFutures.add(future);
							}catch (Exception e) {
								log.warn("TaskUtil callbackedTasks warn: ", e);
							}
						}
					}
					
					if(callbackedFutures != null && callbackedFutures.size() > 0) {
						for(Future<?> future : callbackedFutures) {
							callbackdTasks.remove(future);
						}
					}
				}
			}
		}
	}
	
	/**
	 * 自定义线程名称Task-idx-name-idx2
	 */
	private static class TaskUtilThreadFactory implements ThreadFactory {
		private static AtomicInteger taskutilThreadNumber = new AtomicInteger(1);
		private final String threadNamePrefix;
		TaskUtilThreadFactory(String threadNamePrefix){
			this.threadNamePrefix = threadNamePrefix;
		}
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, String.format("TaskUtil-%d-%s", taskutilThreadNumber.getAndIncrement(), this.threadNamePrefix));
		    t.setDaemon(true);
		    t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	}
	
	/**
	 * 封装定时任务，每次调度时使用cached thread运行，基本不占用调度执行时间
	 * @author hongwei
	 * @date 2014-09-05
	 */
	private static class ScheduleTask implements Runnable {
		private Runnable runner;
		public ScheduleTask(Runnable runnable) {
			this.runner = runnable;
		}
		@Override
		public void run() {
			TaskUtil.submit(runner);
		}
	}
	
	/**
	 * 等待结果回调接口
	 */
	@FunctionalInterface
	public static interface Callback {
		/**
		 * 回调处理结果对象
		 * @param result
		 */
		void handle(Object result);
	}
}
