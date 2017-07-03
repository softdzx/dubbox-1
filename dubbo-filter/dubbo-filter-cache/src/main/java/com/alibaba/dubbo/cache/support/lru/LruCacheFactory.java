package com.alibaba.dubbo.cache.support.lru;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.common.URL;

public class LruCacheFactory extends AbstractCacheFactory {

    protected Cache createCache(URL url) {
        return new LruCache(url);
    }

}
