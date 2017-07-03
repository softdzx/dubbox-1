package com.alibaba.dubbo.cache.filter;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.*;

@Activate(group = {Constants.CONSUMER, Constants.PROVIDER}, value = Constants.CACHE_KEY)
public class CacheFilter implements Filter {

    private CacheFactory cacheFactory;

    public void setCacheFactory(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (cacheFactory != null && ConfigUtils.isNotEmpty(invoker.getUrl().getMethodParameter(invocation.getMethodName(),
                Constants.CACHE_KEY))) {
            Cache cache = cacheFactory.getCache(invoker.getUrl().addParameter(Constants.METHOD_KEY, invocation.getMethodName()));
            if (cache != null) {
                String key = StringUtils.toArgumentString(invocation.getArguments());
                Object value = cache.get(key);
                if (value != null) {
                    return new RpcResult(value);
                }
                Result result = invoker.invoke(invocation);
                if (!result.hasException()) {
                    cache.put(key, result.getValue());
                }
                return result;
            }
        }
        return invoker.invoke(invocation);
    }

}
