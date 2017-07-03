package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Directory;

/**
 * 快速失败，只发起一次调用，失败立即报错，通常用于非幂等性的写操作。
 *  
 * <a href="http://en.wikipedia.org/wiki/Fail-fast">Fail-fast</a>
 * 
 * @author william.liangf
 */
public class FailfastCluster implements Cluster {

    public final static String NAME = "failfast";

    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailfastClusterInvoker<T>(directory);
    }

}