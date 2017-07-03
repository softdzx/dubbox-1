package com.alibaba.dubbo.monitor;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * MonitorFactory. (SPI, Singleton, ThreadSafe)
 *
 * @author william.liangf
 */
@SPI("dubbo")
public interface MonitorFactory {

    /**
     * Create monitor.
     *
     * @param url url
     * @return monitor
     */
    @Adaptive("protocol")
    Monitor getMonitor(URL url);

}