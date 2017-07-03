package com.alibaba.dubbo.container.spring;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.container.Container;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * SpringContainer. (SPI, Singleton, ThreadSafe)
 * 
 * @author william.liangf
 */
public class SpringContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(SpringContainer.class);

    public static final String SPRING_CONFIG = "dubbo.spring.config";
    
    public static final String DEFAULT_SPRING_CONFIG = "classpath*:META-INF/spring/*.xml";

    public static ClassPathXmlApplicationContext context;
    
    public static ClassPathXmlApplicationContext getContext() {
		return context;
	}

	public void start() {
        String configPath = ConfigUtils.getProperty(SPRING_CONFIG);
        if (configPath == null || configPath.length() == 0) {
            configPath = DEFAULT_SPRING_CONFIG;
        }
        context = new ClassPathXmlApplicationContext(configPath.split("[,\\s]+"));
        context.start();
    }

    public void stop() {
        try {
            if (context != null) {
                context.stop();
                context.close();
                context = null;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}