package com.alibaba.dubbo.rpc.cluster.configurator.absent;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.configurator.AbstractConfigurator;

public class AbsentConfigurator extends AbstractConfigurator {

    public AbsentConfigurator(URL url) {
        super(url);
    }

    public URL doConfigure(URL currentUrl, URL configUrl) {
        return currentUrl.addParametersIfAbsent(configUrl.getParameters());
    }

}
