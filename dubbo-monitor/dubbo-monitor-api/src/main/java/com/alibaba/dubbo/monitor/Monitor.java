package com.alibaba.dubbo.monitor;

import com.alibaba.dubbo.common.Node;

/**
 * Monitor. (SPI, Prototype, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.monitor.MonitorFactory#getMonitor(com.alibaba.dubbo.common.URL)
 * @author william.liangf
 */
public interface Monitor extends Node, MonitorService {

}