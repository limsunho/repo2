package org.mybatis.spring;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;

/**
 * MyBatis Mapper XML 파일 Reload 클래스
 * 
 * ※ 운영시에는 사용을 안하는게 좋다.
 * 
 * @author jhkang
 * @since 2012.11.22
 */
public class RefreshableSqlSessionFactoryBean extends SqlSessionFactoryBean implements DisposableBean {

	private static Logger log = LogManager.getLogger(RefreshableSqlSessionFactoryBean.class);
	
	private SqlSessionFactory proxy;
	private int interval = 500;
	
	private Timer timer;
	private TimerTask task;
	
	private Resource[] mapperLocations;
	
	/**
	 * 파일 감시 Thread 실행 여부
	 */
	private boolean running = false;
	
	private final ReentrantReadWriteLock rwt = new ReentrantReadWriteLock();
	private final Lock r = rwt.readLock();
	private final Lock w = rwt.writeLock();
	
	/**
	 * MyBatis Mapper XML 새로 고침
	 * 
	 * @throws Exception
	 */
	public void refresh() throws Exception {
		log.debug("Reloading Mybatis XML...");
		
		w.lock();
		
		try {
			super.afterPropertiesSet();
		} finally {
			w.unlock();
		}
	}
	
	/**
	 * Singleton 멤버로, Proxy로 설정하도록 Override
	 */
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		setRefreshable();
	}
	
	public void setRefreshable() {
		proxy = (SqlSessionFactory) Proxy.newProxyInstance(
					SqlSessionFactory.class.getClassLoader(),
					new Class[] { SqlSessionFactory.class },
					new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							return method.invoke(getParentObject(), args);
						}
					});
		
		task = new TimerTask() {
			private Map<Resource, Long> map = new HashMap<Resource, Long>();
			
			@Override
			public void run() {
				if (isModified()) {
					try {
						refresh();
					} catch (Exception e) {
						log.error("Caught Exception: {}", e);
					}
				}
			}
			
			private boolean isModified() {
				boolean retVal = false;
				
				if (mapperLocations != null) {
					for (int i = 0 ; i < mapperLocations.length ; i++) {
						Resource mappingLocation = mapperLocations[i];
						retVal |= findModifiedResource(mappingLocation);
					}
				}
				
				return retVal;
			}
			
			private boolean findModifiedResource(Resource resource) {
				boolean retVal = false;
				List<String> modifiedResources = new ArrayList<String>();
				
				try {
					long modified = resource.lastModified();
					
					if (map.containsKey(resource)) {
						long lastModified = ((Long) map.get(resource)).longValue();
						
						if (lastModified != modified) {
							map.put(resource, new Long(modified));
							modifiedResources.add(resource.getDescription());
							
							retVal = true;
						}
					} else {
						map.put(resource, new Long(modified));
					}
				} catch (IOException ioe) {
					log.error("Caught Exception: {}", ioe);
				}
				
				if (retVal) {
					log.debug("Modified files: {}", modifiedResources);
				}
				
				return retVal;
			}
		};
		
		timer = new Timer(true);
		resetInterval();
	}
	
	public Object getParentObject() throws Exception {
		r.lock();
		
		try {
			return super.getObject();
		} finally {
			r.unlock();
		}
	}
	
	public SqlSessionFactory getObject() {
		return this.proxy;
	}
	
	public Class <? extends SqlSessionFactory> getObjectType() {
		return (this.proxy != null ? this.proxy.getClass() : SqlSessionFactory.class);
	}
	
	public boolean isSingleton() {
		return true;
	}
	
	public void setMapperLocations(Resource[] mapperLocations) {
		super.setMapperLocations(mapperLocations);
		this.mapperLocations = mapperLocations;
	}
	
	public void setInterval(int interval) {
		this.interval = interval;
	}
	
	public void setCheckInterval(int ms) {
		interval = ms;
		
		if (timer != null) {
			resetInterval();
		}
	}
	
	private void resetInterval() {
		if (running) {
			timer.cancel();
			running = false;
		}
		
		if (interval > 0) {
			timer.schedule(task, 0, interval);
			running = true;
		}
	}
	
	@Override
	public void destroy() throws Exception {
		timer.cancel();
	}

}