package com.alibaba.dubbo.rpc.cluster;

import java.util.List;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * Router. (SPI, Prototype, ThreadSafe)
 * 
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 * 
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.dubbo.rpc.cluster.Directory#list(Invocation)
 * @author chao.liuc
 */
public interface Router extends Comparable<Router> {

    /**
     * get the router url.
     * 
     * @return url
     */
    URL getUrl();

    /**
     * route.
     * 
     * @param invokers
     * @param url refer url
     * @param invocation
     * @return routed invokers
     * @throws RpcException
     */
	<T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;

}