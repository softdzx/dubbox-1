package com.alibaba.dubbo.cache;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

@SPI("lru")
public interface CacheFactory {

    @Adaptive("cache")
    Cache getCache(URL url);

}
