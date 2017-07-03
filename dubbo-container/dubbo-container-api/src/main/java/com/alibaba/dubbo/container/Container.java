package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * Container. (SPI, Singleton, ThreadSafe)
 * 
 * @author william.liangf
 */
@SPI("spring")
public interface Container {
    
    /**
     * start.
     */
    void start();
    
    /**
     * stop.
     */
    void stop();

}