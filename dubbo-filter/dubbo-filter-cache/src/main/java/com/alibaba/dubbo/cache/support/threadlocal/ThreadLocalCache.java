package com.alibaba.dubbo.cache.support.threadlocal;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;

public class ThreadLocalCache implements Cache {

    private final ThreadLocal<Map<Object, Object>> store;

    public ThreadLocalCache(URL url) {
        this.store = ThreadLocal.withInitial(HashMap::new);
    }

    public void put(Object key, Object value) {
        store.get().put(key, value);
    }

    public Object get(Object key) {
        return store.get().get(key);
    }

}
