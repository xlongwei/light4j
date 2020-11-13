package ch.qos.logback.classic.redis;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.util.Duration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

public class RedisAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	boolean pubsub = true;//发送方publish，接收方subscribe
	boolean pushpop = false;//发送方lpush（使用lrange限制长度），接收方rbpop（阻塞）
	int queueSize = 10240;//接收方异常时，缓存queueSize条日志
	byte[] key = "logserver".getBytes(StandardCharsets.UTF_8);
	String host = "localhost";
	int port = Protocol.DEFAULT_PORT;
	Duration reconnectionDelay = new Duration(10000);
	BlockingQueue<byte[]> blockingQueue = null;
	JedisPool pool = null;
	Jedis client = null;
	Method returnBrokenResource = null;
	ExecutorService es = null;
	Boolean ltrim = null;

	@Override
	protected void append(ILoggingEvent event) {
		if(!pubsub && !pushpop) {
			return;
		}
		byte[] message = serialize(event);
		offer(message);
	}
	
	private void offer(byte[] message) {
		if(message != null) {
			boolean offer = blockingQueue.offer(message);
			if(offer == false) {
				blockingQueue.poll();
				blockingQueue.offer(message);
			}
		}
	}
	
	private void returnBrokenResource() {
//		if(returnBrokenResource==null) {
//			try {
//				returnBrokenResource = JedisPool.class.getDeclaredMethod("returnBrokenResource", Jedis.class);
//				returnBrokenResource.setAccessible(true);
//			}catch(Exception e) {
//				System.err.println("fail to get method returnBrokenResource: "+e.getMessage());
//			}
//		}
		try {
			//针对jedis不同版本，可以直接调用close或returnBrokenResource方法，则注释掉反射代码即可
//			returnBrokenResource.invoke(pool, client);
//			client.close();
			pool.returnBrokenResource(client);
		}catch(Exception e) {
			System.err.println("fail to returnBrokenResource: "+e.getMessage());
		}
		try{
			Thread.sleep(reconnectionDelay.getMilliseconds());
		}catch(InterruptedException e) {
			System.err.println("interrupt returnBrokenResource: "+e.getMessage());
		}
	}
	
	private void getResource() {
		for(int i=0; i < 3; i++) {
			try {
				Future<Jedis> future = es.submit(() -> {
					if(pool == null) {
						pool = new JedisPool(host, port);
					}
					return pool.getResource();
				});
				client = future.get(3, TimeUnit.SECONDS);
				return;
			}catch(Throwable e) {
				System.err.println("fail to getResource: "+e.getMessage());
				returnBrokenResource();
			}
		}
	}
	
	private static byte[] serialize(ILoggingEvent event) {
		Serializable obj = null;
		if (event instanceof Serializable) {
			obj = (Serializable) event;
		} else if (event instanceof LoggingEvent) {
			obj = LoggingEventVO.build(event);
		}
		if (obj == null) {
			return null;
		}
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
			oos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setPubsub(boolean pubsub) {
		this.pubsub = pubsub;
	}

	public void setPushpop(boolean pushpop) {
		this.pushpop = pushpop;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public void setKey(String key) {
		this.key = key.getBytes(StandardCharsets.UTF_8);
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setReconnectionDelay(Duration reconnectionDelay) {
		this.reconnectionDelay = reconnectionDelay;
	}

	@Override
	public void start() {
		super.start();
		if(pubsub || pushpop) {
			blockingQueue = new ArrayBlockingQueue<>(queueSize);
			LoggerContext lc = (LoggerContext) getContext();
			es = lc.getExecutorService();
			es.submit(() -> {
				getResource();
				while(true) {
					try {
						byte[] message = blockingQueue.take();
						if(pubsub) {
							try{
//								es.submit(() -> {
									client.publish(key, message);
//								}).get(3, TimeUnit.SECONDS);
							}catch(Throwable e) {
								System.err.println("fail to publish: "+e.getMessage());
								returnBrokenResource();
								getResource();
							}
						}
						if(pushpop) {
							try{
								client.lpush(key, message);
								if(queueSize > 0) {
									if(ltrim == null) {
										try {
//											es.submit(() -> {
												client.ltrim(key, 0, queueSize);
//											}).get(3, TimeUnit.SECONDS);
											ltrim = Boolean.TRUE;
										}catch(Throwable t) {
											System.err.println("fail to ltrim: "+t.getMessage());
											ltrim = Boolean.FALSE;
										}
									}else if(ltrim.booleanValue()){
										client.ltrim(key, 0, queueSize);
									}
								}
							}catch(Throwable e) {
								System.err.println("fail to lpush: "+e.getMessage());
								returnBrokenResource();
								getResource();
							}
						}						
					}catch(InterruptedException e) {
						
					}
				}				
			});
		}
	}

	@Override
	public void stop() {
		super.stop();
		if(pool != null) {
			pool.destroy();
		}
	}

}
