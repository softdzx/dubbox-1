package com.alibaba.dubbo.rpc.cluster;

import java.util.List;

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * Directory. (SPI, Prototype, ThreadSafe)
 * 
 * <a href="http://en.wikipedia.org/wiki/Directory_service">Directory Service</a>
 * 
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 * @author william.liangf
 */
public interface Directory<T> extends Node {
    
    /**
     * get service type.
     * 
     * @return service type.
     */
    Class<T> getInterface();

    /**
     * list invokers.
     * 
     * @return invokers
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;
    
}