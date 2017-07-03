package com.alibaba.dubbo.cache.support.threadlocal;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.common.URL;

public class ThreadLocalCacheFactory extends AbstractCacheFactory {

    protected Cache createCache(URL url) {
        return new ThreadLocalCache(url);
    }

}
