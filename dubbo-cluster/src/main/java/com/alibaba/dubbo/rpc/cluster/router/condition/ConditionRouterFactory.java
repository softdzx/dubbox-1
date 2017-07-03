package com.alibaba.dubbo.rpc.cluster.router.condition;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;

public class ConditionRouterFactory implements RouterFactory {

    public static final String NAME = "condition";

    public Router getRouter(URL url) {
        return new ConditionRouter(url);
    }

}