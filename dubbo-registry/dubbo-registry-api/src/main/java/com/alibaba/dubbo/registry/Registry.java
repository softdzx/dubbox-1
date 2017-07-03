package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.common.URL;

/**
 * Registry. (SPI, Prototype, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.registry.RegistryFactory#getRegistry(URL)
 * @see com.alibaba.dubbo.registry.support.AbstractRegistry
 * @author william.liangf
 */
public interface Registry extends Node, RegistryService {
}