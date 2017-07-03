package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.support.MockInvoker;

import java.util.List;

public class MockClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(MockClusterInvoker.class);

    private final Directory<T> directory;

    private final Invoker<T> invoker;

    public MockClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
    }

    public URL getUrl() {
        return directory.getUrl();
    }

    public boolean isAvailable() {
        return directory.isAvailable();
    }

    public void destroy() {
        this.invoker.destroy();
    }

    public Class<T> getInterface() {
        return directory.getInterface();
    }

    public Result invoke(Invocation invocation) throws RpcException {
        Result result = null;

        String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim();
        if (value.length() == 0 || value.equalsIgnoreCase("false")) {
            //no mock
            result = this.invoker.invoke(invocation);
        } else if (value.startsWith("force")) {
            LogHelper.info(logger, "force-mock: " + invocation.getMethodName() + " force-mock enabled , url : " + directory.getUrl());
            //force:direct mock
            result = doMockInvoke(invocation, null);
        } else {
            //fail-mock
            try {
                result = this.invoker.invoke(invocation);
            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                } else {
                    LogHelper.info(logger, "fail-mock: " + invocation.getMethodName() + " fail-mock enabled , url : " + directory.getUrl(), e);
                    result = doMockInvoke(invocation, e);
                }
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result doMockInvoke(Invocation invocation, RpcException e) {
        Result result = null;
        List<Invoker<T>> mockInvokers = selectMockInvoker(invocation);
        Invoker<T> minvoker = CollectionUtils.isEmpty(mockInvokers) ? (Invoker<T>) new MockInvoker(directory.getUrl()) :
                mockInvokers.get(0);
        try {
            result = minvoker.invoke(invocation);
        } catch (RpcException me) {
            if (!me.isBiz()) {
                throw new RpcException(me.getCode(), getMockExceptionMessage(e, me), me.getCause());
            }
            result = new RpcResult(me.getCause());
        } catch (Throwable me) {
            throw new RpcException(getMockExceptionMessage(e, me), me.getCause());
        }
        return result;
    }

    private String getMockExceptionMessage(Throwable t, Throwable mt) {
        String msg = "mock error : " + mt.getMessage();
        if (t != null) {
            msg = msg + ", invoke error is :" + StringUtils.toString(t);
        }
        return msg;
    }

    /**
     * 返回MockInvoker
     * 契约：
     * directory根据invocation中是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是一个normal invoker 还是一个 mock invoker
     * 如果directorylist 返回多个mock invoker，只使用第一个invoker.
     */
    private List<Invoker<T>> selectMockInvoker(Invocation invocation) {
        //TODO generic invoker？
        if (invocation instanceof RpcInvocation) {
            //存在隐含契约(虽然在接口声明中增加描述，但扩展性会存在问题.同时放在attachement中的做法需要改进
            ((RpcInvocation) invocation).setAttachment(Constants.INVOCATION_NEED_MOCK, Boolean.TRUE.toString());
            //directory根据invocation中attachment是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是normal invokers or mock invokers
            List<Invoker<T>> invokers = directory.list(invocation);
            return invokers;
        }
        return null;
    }

    @Override
    public String toString() {
        return "invoker :" + this.invoker + ",directory: " + this.directory;
    }
}