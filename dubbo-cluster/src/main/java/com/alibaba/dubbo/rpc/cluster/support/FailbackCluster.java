package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Directory;

/**
 * 失败自动恢复，后台记录失败请求，定时重发，通常用于消息通知操作。
 * 
 * <a href="http://en.wikipedia.org/wiki/Failback">Failback</a>
 * 
 * @author william.liangf
 */
public class FailbackCluster implements Cluster {

    public final static String NAME = "failback";    

    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailbackClusterInvoker<T>(directory);
    }

}