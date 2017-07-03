package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * ConfiguratorFactory. (SPI, Singleton, ThreadSafe)
 */
@SPI
public interface ConfiguratorFactory {

    /**
     * get the configurator instance.
     * 
     * @param url - configurator url.
     * @return configurator instance.
     */
    @Adaptive("protocol")
    Configurator getConfigurator(URL url);

}
