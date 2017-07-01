package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.registry.NotifyListener;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * FailbackRegistry. (SPI, Prototype, ThreadSafe)
 *
 * @author william.liangf
 */
public abstract class FailbackRegistry extends AbstractRegistry {

    // 定时任务执行器
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));

    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private final ScheduledFuture<?> retryFuture;

    private final Set<URL> failedRegistered = CollectionUtils.newConcurrentSet();

    private final Set<URL> failedUnregistered = CollectionUtils.newConcurrentSet();

    private final ConcurrentMap<URL, Set<NotifyListener>> failedSubscribed = Maps.newConcurrentMap();

    private final ConcurrentMap<URL, Set<NotifyListener>> failedUnSubscribed = Maps.newConcurrentMap();

    private final ConcurrentMap<URL, Map<NotifyListener, List<URL>>> failedNotified = Maps.newConcurrentMap();

    public FailbackRegistry(URL url) {
        super(url);
        int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(() -> {
            // 检测并连接注册中心
            try {
                retry();
            } catch (Throwable t) { // 防御性容错
                LogHelper.error(logger, "Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    public Future<?> getRetryFuture() {
        return retryFuture;
    }

    public Set<URL> getFailedRegistered() {
        return failedRegistered;
    }

    public Set<URL> getFailedUnregistered() {
        return failedUnregistered;
    }

    public Map<URL, Set<NotifyListener>> getFailedSubscribed() {
        return failedSubscribed;
    }

    public Map<URL, Set<NotifyListener>> getFailedUnSubscribed() {
        return failedUnSubscribed;
    }

    public Map<URL, Map<NotifyListener, List<URL>>> getFailedNotified() {
        return failedNotified;
    }

    private void addFailedSubscribed(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners == null) {
            failedSubscribed.putIfAbsent(url, CollectionUtils.newConcurrentSet());
            listeners = failedSubscribed.get(url);
        }
        listeners.add(listener);
    }

    private void removeFailedSubscribed(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        Map<NotifyListener, List<URL>> notified = failedNotified.get(url);
        if (notified != null) {
            notified.remove(listener);
        }
    }

    @Override
    public void register(URL url) {
        super.register(url);
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            // 向服务器端发送注册请求
            doRegister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailBack = t instanceof SkipFailbackWrapperException;
            if (check || skipFailBack) {
                if (skipFailBack) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to register " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            }
            LogHelper.error(logger, "Failed to register " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            // 将失败的注册请求记录到失败列表，定时重试
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(URL url) {
        super.unregister(url);
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            // 向服务器端发送取消注册请求
            doUnregister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailBack = t instanceof SkipFailbackWrapperException;
            if (check || skipFailBack) {
                if (skipFailBack) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unregister " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            }
            LogHelper.error(logger, "Failed to unRegister " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            // 将失败的取消注册请求记录到失败列表，定时重试
            failedUnregistered.add(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        super.subscribe(url, listener);
        removeFailedSubscribed(url, listener);
        try {
            // 向服务器端发送订阅请求
            doSubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            List<URL> urls = getCacheUrls(url);
            if (!CollectionUtils.isEmpty(urls)) {
                notify(url, listener, urls);
                LogHelper.error(logger, "Failed to subscribe " + url + ", Using cached list: " + urls
                        + " from cache file: " + getUrl().getParameter(Constants.FILE_KEY,
                        System.getProperty("user.home") + "/dubbo-registry-" + url.getHost()
                                + ".cache") + ", cause: " + t.getMessage(), t);
            } else {
                // 如果开启了启动时检测，则直接抛出异常
                boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                        && url.getParameter(Constants.CHECK_KEY, true);
                boolean skipFailBack = t instanceof SkipFailbackWrapperException;
                if (check || skipFailBack) {
                    if (skipFailBack) {
                        t = t.getCause();
                    }
                    throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
                }
                LogHelper.error(logger, "Failed to subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的订阅请求记录到失败列表，定时重试
            addFailedSubscribed(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        removeFailedSubscribed(url, listener);
        try {
            // 向服务器端发送取消订阅请求
            doUnsubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true);
            boolean skipFailBack = t instanceof SkipFailbackWrapperException;
            if (check || skipFailBack) {
                if (skipFailBack) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unSubscribe " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            }
            LogHelper.error(logger, "Failed to unSubscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            // 将失败的取消订阅请求记录到失败列表，定时重试
            Set<NotifyListener> listeners = failedUnSubscribed.get(url);
            if (listeners == null) {
                failedUnSubscribed.putIfAbsent(url, CollectionUtils.newConcurrentSet());
                listeners = failedUnSubscribed.get(url);
            }
            listeners.add(listener);
        }
    }

    @Override
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        try {
            doNotify(url, listener, urls);
        } catch (Exception t) {
            // 将失败的通知请求记录到失败列表，定时重试
            Map<NotifyListener, List<URL>> listeners = failedNotified.get(url);
            if (listeners == null) {
                failedNotified.putIfAbsent(url, Maps.newConcurrentMap());
                listeners = failedNotified.get(url);
            }
            listeners.put(listener, urls);
            LogHelper.error(logger, "Failed to notify for subscribe " + url + ", waiting for retry, cause: "
                    + t.getMessage(), t);
        }
    }

    protected void doNotify(URL url, NotifyListener listener, List<URL> urls) {
        super.notify(url, listener, urls);
    }

    @Override
    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = Sets.newHashSet(getRegistered());
        if (!CollectionUtils.isEmpty(recoverRegistered)) {
            LogHelper.info(logger, "Recover register url " + recoverRegistered);
            failedRegistered.addAll(recoverRegistered);
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = Maps.newHashMap(getSubscribed());
        if (!CollectionUtils.isEmpty(recoverSubscribed)) {
            LogHelper.info(logger, "Recover subscribe url " + recoverSubscribed.keySet());
            recoverSubscribed.forEach((url, listeners) -> listeners.forEach(listener ->
                    addFailedSubscribed(url, listener)));
        }
    }

    // 重试失败的动作
    protected void retry() {
        if (!failedRegistered.isEmpty()) {
            Set<URL> failed = Sets.newHashSet(failedRegistered);
            if (failed.size() > 0) {
                LogHelper.info(logger, "Retry register " + failed);
                try {
                    failed.forEach(url -> {
                        doRegister(url);
                        failedRegistered.remove(url);
                    });
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    LogHelper.warn(logger, "Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (!failedUnregistered.isEmpty()) {
            Set<URL> failed = Sets.newHashSet(failedUnregistered);
            if (failed.size() > 0) {
                LogHelper.info(logger, "Retry unregister " + failed);
                try {
                    failed.forEach(url -> {
                        doUnregister(url);
                        failedUnregistered.remove(url);
                    });
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    LogHelper.warn(logger, "Failed to retry unregister  " + failed + ", waiting for again, cause: "
                            + t.getMessage(), t);
                }
            }
        }
        if (!failedSubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = Maps.newHashMap(failedSubscribed);
            Maps.newHashMap(failed).forEach((url, notifyListeners) -> {
                if (CollectionUtils.isEmpty(notifyListeners))
                    failed.remove(url);
            });
            if (failed.size() > 0) {
                LogHelper.info(logger, "Retry subscribe " + failed);
                try {
                    failed.forEach((url, listeners) -> listeners.forEach(listener -> {
                        doSubscribe(url, listener);
                        listeners.remove(listener);
                    }));
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    LogHelper.warn(logger, "Failed to retry subscribe " + failed + ", waiting for again, cause: "
                            + t.getMessage(), t);
                }
            }
        }
        if (!failedUnSubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = Maps.newHashMap(failedUnSubscribed);
            Maps.newHashMap(failed).forEach((url, notifyListeners) -> {
                if (CollectionUtils.isEmpty(notifyListeners))
                    failed.remove(url);
            });
            if (failed.size() > 0) {
                LogHelper.info(logger, "Retry unSubscribe " + failed);
                try {
                    failed.forEach((url, listeners) -> listeners.forEach(listener -> {
                        doUnsubscribe(url, listener);
                        listeners.remove(listener);
                    }));
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    LogHelper.warn(logger, "Failed to retry unSubscribe " + failed + ", waiting for again, cause: "
                            + t.getMessage(), t);
                }
            }
        }
        if (!failedNotified.isEmpty()) {
            Map<URL, Map<NotifyListener, List<URL>>> failed = Maps.newHashMap(failedNotified);
            Maps.newHashMap(failed).forEach((url, notifyListeners) -> {
                if (CollectionUtils.isEmpty(notifyListeners))
                    failed.remove(url);
            });
            if (failed.size() > 0) {
                LogHelper.info(logger, "Retry notify " + failed);
                try {
                    failed.forEach((url, listeners) -> listeners.forEach((listener, urls) -> {
                        listener.notify(urls);
                        listeners.remove(listener);
                    }));
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    LogHelper.warn(logger, "Failed to retry notify " + failed + ", waiting for again, cause: "
                            + t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            retryFuture.cancel(true);
        } catch (Throwable t) {
            LogHelper.warn(logger, t.getMessage(), t);
        }
    }

    // ==== 模板方法 ====

    protected abstract void doRegister(URL url);

    protected abstract void doUnregister(URL url);

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

}