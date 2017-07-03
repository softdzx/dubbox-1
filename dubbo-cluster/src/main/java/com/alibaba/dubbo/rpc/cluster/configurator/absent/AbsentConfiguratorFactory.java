package com.alibaba.dubbo.rpc.cluster.configurator.absent;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;

public class AbsentConfiguratorFactory implements ConfiguratorFactory {

    public Configurator getConfigurator(URL url) {
        return new AbsentConfigurator(url);
    }

}
