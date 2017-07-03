package com.alibaba.dubbo.cache.support;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.common.URL;
import com.google.common.collect.Maps;

import java.util.concurrent.ConcurrentMap;

public abstract class AbstractCacheFactory implements CacheFactory {

    private final ConcurrentMap<String, Cache> caches = Maps.newConcurrentMap();

    public Cache getCache(URL url) {
        String key = url.toFullString();
        Cache cache = caches.get(key);
        if (cache == null) {
            caches.put(key, createCache(url));
            cache = caches.get(key);
        }
        return cache;
    }

    protected abstract Cache createCache(URL url);

}
