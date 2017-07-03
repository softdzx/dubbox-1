package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.Merger;
import com.alibaba.dubbo.rpc.cluster.merger.MergerFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@SuppressWarnings("unchecked")
public class MergeableClusterInvoker<T> implements Invoker<T> {

    private static final Logger log = LoggerFactory.getLogger(MergeableClusterInvoker.class);

    private ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("mergeable-cluster-executor", true));

    private final Directory<T> directory;

    public MergeableClusterInvoker(Directory<T> directory) {
        this.directory = directory;
    }

    @SuppressWarnings("rawtypes")
    public Result invoke(final Invocation invocation) throws RpcException {
        List<Invoker<T>> invokers = directory.list(invocation);

        String merger = getUrl().getMethodParameter(invocation.getMethodName(), Constants.MERGER_KEY);
        if (ConfigUtils.isEmpty(merger)) { // 如果方法不需要Merge，退化为只调一个Group
            for (final Invoker<T> invoker : invokers) {
                if (invoker.isAvailable()) {
                    return invoker.invoke(invocation);
                }
            }
            return invokers.iterator().next().invoke(invocation);
        }

        Class<?> returnType;
        try {
            returnType = getInterface().getMethod(
                    invocation.getMethodName(), invocation.getParameterTypes()).getReturnType();
        } catch (NoSuchMethodException e) {
            returnType = null;
        }

        Map<String, Future<Result>> results = Maps.newHashMap();
        for (final Invoker<T> invoker : invokers) {
            Future<Result> future = executor.submit(new Callable<Result>() {
                public Result call() throws Exception {
                    return invoker.invoke(new RpcInvocation(invocation, invoker));
                }
            });
            results.put(invoker.getUrl().getServiceKey(), future);
        }

        Object result = null;

        List<Result> resultList = Lists.newArrayListWithCapacity(results.size());

        int timeout = getUrl().getMethodParameter(invocation.getMethodName(), Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        for (Map.Entry<String, Future<Result>> entry : results.entrySet()) {
            Future<Result> future = entry.getValue();
            try {
                Result r = future.get(timeout, TimeUnit.MILLISECONDS);
                if (r.hasException()) {
                    LogHelper.error(log, "Invoke " + getGroupDescFromServiceKey(entry.getKey()) + " failed: " +
                            r.getException().getMessage(), r.getException());
                } else {
                    resultList.add(r);
                }
            } catch (Exception e) {
                throw new RpcException("Failed to invoke service " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }

        if (resultList.size() == 0) {
            return new RpcResult((Object) null);
        } else if (resultList.size() == 1) {
            return resultList.iterator().next();
        }

        if (returnType == void.class) {
            return new RpcResult((Object) null);
        }

        if (merger.startsWith(".")) {
            merger = merger.substring(1);
            Method method;
            try {
                method = returnType.getMethod(merger, returnType);
            } catch (NoSuchMethodException e) {
                throw new RpcException("Can not merge result because missing method [ " + merger + " ] in class [ "
                        + returnType.getClass().getName() + " ]");
            }
            if (null == method)
                throw new RpcException("Can not merge result because missing method [ " + merger + " ] in class [ "
                        + returnType.getClass().getName() + " ]");
            if (!Modifier.isPublic(method.getModifiers())) {
                method.setAccessible(true);
            }
            result = resultList.remove(0).getValue();
            try {
                if (method.getReturnType() != void.class
                        && method.getReturnType().isAssignableFrom(result.getClass())) {
                    for (Result r : resultList) {
                        result = method.invoke(result, r.getValue());
                    }
                } else {
                    for (Result r : resultList) {
                        method.invoke(result, r.getValue());
                    }
                }
            } catch (Exception e) {
                throw new RpcException("Can not merge result: " + e.getMessage());
            }
        } else {
            Merger resultMerger = ConfigUtils.isDefault(merger) ? MergerFactory.getMerger(returnType) :
                    ExtensionLoader.getExtensionLoader(Merger.class).getExtension(merger);
            if (null == resultMerger) {
                throw new RpcException("There is no merger to merge result.");
            }
            List<Object> rets = Lists.newArrayListWithCapacity(resultList.size());
            for (Result r : resultList) {
                rets.add(r.getValue());
            }
            result = resultMerger.merge(rets.toArray((Object[]) Array.newInstance(returnType, 0)));
        }
        return new RpcResult(result);
    }

    public Class<T> getInterface() {
        return directory.getInterface();
    }

    public URL getUrl() {
        return directory.getUrl();
    }

    public boolean isAvailable() {
        return directory.isAvailable();
    }

    public void destroy() {
        directory.destroy();
    }

    private String getGroupDescFromServiceKey(String key) {
        int index = key.indexOf("/");
        return index > 0 ? "group [ " + key.substring(0, index) + " ]" : key;
    }
}
