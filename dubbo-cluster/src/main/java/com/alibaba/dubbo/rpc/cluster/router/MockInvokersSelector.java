package com.alibaba.dubbo.rpc.cluster.router;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * mock invoker选择器
 *
 * @author chao.liuc
 */
public class MockInvokersSelector implements Router {

    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers,
                                      URL url, final Invocation invocation) throws RpcException {
        if (invocation.getAttachments() == null) {
            return getNormalInvokers(invokers);
        }
        String value = invocation.getAttachments().get(Constants.INVOCATION_NEED_MOCK);
        if (value == null)
            return getNormalInvokers(invokers);
        else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
            return getMockedInvokers(invokers);
        }
        return invokers;
    }

    private <T> List<Invoker<T>> getMockedInvokers(final List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)) {
            return null;
        }
        List<Invoker<T>> sInvokers = Lists.newArrayListWithCapacity(1);
        for (Invoker<T> invoker : invokers) {
            if (Constants.MOCK_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
                sInvokers.add(invoker);
            }
        }
        return sInvokers;
    }

    private <T> List<Invoker<T>> getNormalInvokers(final List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)) {
            return invokers;
        }
        List<Invoker<T>> sInvokers = Lists.newArrayListWithCapacity(invokers.size());
        for (Invoker<T> invoker : invokers) {
            if (!Constants.MOCK_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
                sInvokers.add(invoker);
            }
        }
        return sInvokers;
    }

    private <T> boolean hasMockProviders(final List<Invoker<T>> invokers) {
        boolean hasMockProvider = false;
        for (Invoker<T> invoker : invokers) {
            if (Constants.MOCK_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
                hasMockProvider = true;
                break;
            }
        }
        return hasMockProvider;
    }

    public URL getUrl() {
        return null;
    }

    public int compareTo(Router o) {
        return 1;
    }

}
