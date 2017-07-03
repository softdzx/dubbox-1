package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.concurrent.*;

/**
 * 失败自动恢复，后台记录失败请求，定时重发，通常用于消息通知操作。
 *
 * @author tony.chenl
 */
public class FailbackClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailbackClusterInvoker.class);

    private static final long RETRY_FAILED_PERIOD = 5 * 1000;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2,
            new NamedThreadFactory("failback-cluster-timer", true));

    private volatile ScheduledFuture<?> retryFuture;

    private final ConcurrentMap<Invocation, AbstractClusterInvoker<?>> failed = Maps.newConcurrentMap();

    public FailbackClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    private void addFailed(Invocation invocation, AbstractClusterInvoker<?> router) {
        if (retryFuture == null) {
            synchronized (this) {
                if (retryFuture == null) {
                    retryFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
                        // 收集统计信息
                        try {
                            retryFailed();
                        } catch (Throwable t) { // 防御性容错
                            LogHelper.error(logger, "Unexpected error occur at collect statistic", t);
                        }
                    }, RETRY_FAILED_PERIOD, RETRY_FAILED_PERIOD, TimeUnit.MILLISECONDS);
                }
            }
        }
        failed.put(invocation, router);
    }

    void retryFailed() {
        if (failed.size() == 0) {
            return;
        }
        Maps.newHashMap(failed).forEach((invocation, invoker) -> {
            try {
                invoker.invoke(invocation);
                failed.remove(invocation);
            } catch (Throwable e) {
                LogHelper.error(logger, "Failed retry to invoke method " + invocation.getMethodName() + ", waiting again.", e);
            }
        });
    }

    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            LogHelper.error(logger, "Failback to invoke method " + invocation.getMethodName() + ", wait for retry in background. Ignored exception: "
                    + e.getMessage() + ", ", e);
            addFailed(invocation, this);
            return new RpcResult(); // ignore
        }
    }

}