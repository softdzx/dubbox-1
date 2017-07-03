package com.alibaba.dubbo.monitor.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractMonitorFactroy. (SPI, Singleton, ThreadSafe)
 *
 * @author william.liangf
 */
public abstract class AbstractMonitorFactory implements MonitorFactory {

    // 注册中心获取过程锁
    private static final ReentrantLock LOCK = new ReentrantLock();

    // 注册中心集合 Map<RegistryAddress, Registry>
    private static final Map<String, Monitor> MONITORS = Maps.newConcurrentMap();

    public static Collection<Monitor> getMonitors() {
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    public Monitor getMonitor(URL url) {
        url = url.setPath(MonitorService.class.getName()).addParameter(Constants.INTERFACE_KEY, MonitorService.class.getName());
        String key = url.toServiceString();
        LOCK.lock();
        try {
            Monitor monitor = MONITORS.get(key);
            if (monitor != null) {
                return monitor;
            }
            monitor = createMonitor(url);
            if (monitor == null) {
                throw new IllegalStateException("Can not create monitor " + url);
            }
            MONITORS.put(key, monitor);
            return monitor;
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    protected abstract Monitor createMonitor(URL url);

}