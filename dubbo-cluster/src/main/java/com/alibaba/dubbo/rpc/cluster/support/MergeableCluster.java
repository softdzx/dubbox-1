package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Directory;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class MergeableCluster implements Cluster {

    public static final String NAME = "mergeable";

    public <T> Invoker<T> join( Directory<T> directory ) throws RpcException {
        return new MergeableClusterInvoker<T>( directory );
    }

}
