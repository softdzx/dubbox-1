package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.*;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractRegistry. (SPI, Prototype, ThreadSafe)
 *
 * @author chao.liuc
 * @author william.liangf
 */
public abstract class AbstractRegistry implements Registry {

    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // URL地址分隔符，用于文件缓存中，服务提供者URL分隔
    private static final char URL_SEPARATOR = ' ';

    // URL地址分隔正则表达式，用于解析文件缓存中服务提供者URL列表
    private static final String URL_SPLIT = "\\s+";

    private URL registryUrl;

    // 本地磁盘缓存文件
    private File file;

    // 本地磁盘缓存，其中特殊的key值.registries记录注册中心列表，其它均为notified服务提供者列表
    private final Properties properties = new Properties();

    // 文件缓存定时写入
    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1,
            new NamedThreadFactory("DubboSaveRegistryCache", true));

    //是否是同步保存文件
    private final boolean syncSaveFile;

    private final AtomicLong lastCacheChanged = new AtomicLong();

    private final Set<URL> registered = new ConcurrentHashSet<>();

    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = Maps.newConcurrentMap();

    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = Maps.newConcurrentMap();

    public AbstractRegistry(URL url) {
        setUrl(url);
        // 启动文件保存定时器
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);
        String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") +
                "/.dubbo/dubbo-registry-" + url.getHost() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid registry store file " + file +
                            ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file;
        loadProperties();
        notify(url.getBackupUrls());
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public URL getUrl() {
        return registryUrl;
    }

    public Set<URL> getRegistered() {
        return registered;
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public Map<URL, Map<String, List<URL>>> getNotified() {
        return notified;
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged() {
        return lastCacheChanged;
    }

    private class SaveProperties implements Runnable {
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        public void run() {
            doSaveProperties(version);
        }
    }

    public void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        Properties newProperties = new Properties();
        // 保存之前先读取一遍，防止多个注册中心之间冲突
        InputStream in = null;
        try {
            if (file.exists()) {
                in = new FileInputStream(file);
                newProperties.load(in);
            }
        } catch (Throwable e) {
            LogHelper.warn(logger, "Failed to load registry store file, cause: " + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LogHelper.warn(logger, e.getMessage(), e);
                }
            }
        }
        // 保存
        try {
            newProperties.putAll(properties);
            File lockFile = new File(file.getAbsolutePath() + ".lock");
            if (!lockFile.exists()) {
                lockFile.createNewFile();
            }
            try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw")) {
                try (FileChannel channel = raf.getChannel()) {
                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() +
                                ", ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");
                    }
                    // 保存
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        try (FileOutputStream outputFile = new FileOutputStream(file)) {
                            newProperties.store(outputFile, "Dubbo Registry Cache");
                        }
                    } finally {
                        lock.release();
                    }
                }
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            LogHelper.warn(logger, "Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }

    private void loadProperties() {
        if (file != null && file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                properties.load(in);
                LogHelper.info(logger, "Load registry store file " + file + ", data: " + properties);
            } catch (Throwable e) {
                LogHelper.warn(logger, "Failed to load registry store file " + file, e);
            }
        }
    }

    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!Strings.isNullOrEmpty(key) && key.equals(url.getServiceKey())
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_')
                    && !Strings.isNullOrEmpty(value)) {
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = Lists.newArrayList();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    public List<URL> lookup(URL url) {
        List<URL> result = Lists.newArrayList();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (!CollectionUtils.isEmpty(notifiedUrls)) {
            notifiedUrls.values().forEach(urls -> urls.forEach(u -> {
                if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                    result.add(u);
                }
            }));
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<>();
            NotifyListener listener = (urls) -> reference.set(urls);
            subscribe(url, listener); // 订阅逻辑保证第一次notify后再返回
            List<URL> urls = reference.get();
            if (!CollectionUtils.isEmpty(urls)) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
    }

    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        LogHelper.info(logger, "Register: " + url);
        registered.add(url);
    }

    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        LogHelper.info(logger, "Unregister: " + url);
        registered.remove(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        LogHelper.info(logger, "Subscribe: " + url);
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unSubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unSubscribe listener == null");
        }
        LogHelper.info(logger, "Unregister: " + url);
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = Sets.newHashSet(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            LogHelper.info(logger, "Recover register url " + recoverRegistered);
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = Maps.newHashMap(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            LogHelper.info(logger, "Recover subscribe url " + recoverSubscribed.keySet());
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }

    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            List<URL> result = Lists.newArrayListWithCapacity(1);
            result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }

    protected void notify(List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) return;

        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();

            if (!UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }

            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        LogHelper.error(logger, "Failed to notify registry event, urls: " + urls + ", cause: " +
                                t.getMessage(), t);
                    }
                }
            }
        }
    }

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if (CollectionUtils.isEmpty(urls)
                && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            LogHelper.warn(logger, "Ignore empty notify urls for subscribe url " + url);
            return;
        }
        LogHelper.info(logger, "Notify urls for subscribe url " + url + ", urls: " + urls);
        Map<String, List<URL>> result = Maps.newHashMap();
        for (URL u : urls) {
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
//                List<URL> categoryList = result.putIfAbsent(category, Lists.newArrayList());
                List<URL> categoryList = result.get(category);
                if (categoryList == null) {
                    categoryList = Lists.newArrayList();
                    result.put(category, categoryList);
                }
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null) {
            notified.putIfAbsent(url, Maps.newConcurrentMap());
            categoryNotified = notified.get(url);
        }
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList);
            saveProperties(url);
            listener.notify(categoryList);
        }
    }

    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }

        try {
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            properties.setProperty(url.getServiceKey(), buf.toString());
            long version = lastCacheChanged.incrementAndGet();
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            LogHelper.warn(logger, t.getMessage(), t);
        }
    }

    public void destroy() {
        LogHelper.info(logger, "Destroy registry:" + getUrl());
        Set<URL> destroyRegistered = Sets.newHashSet(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : Sets.newHashSet(getRegistered())) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                    try {
                        unregister(url);
                        LogHelper.info(logger, "Destroy unregister url " + url);
                    } catch (Throwable t) {
                        LogHelper.warn(logger, "Failed to unregister url " + url + " to registry " + getUrl() +
                                " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = Maps.newHashMap(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        LogHelper.info(logger, "Destroy unSubscribe url " + url);
                    } catch (Throwable t) {
                        LogHelper.warn(logger, "Failed to unSubscribe url " + url + " to registry " + getUrl() +
                                " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String toString() {
        return getUrl().toString();
    }

}