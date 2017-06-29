package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * abstract ProtocolSupport.
 *
 * @author qian.lei
 * @author william.liangf
 */
public abstract class AbstractProtocol implements Protocol {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, Exporter<?>> exporterMap = Maps.newConcurrentMap();

    //TODO SOFEREFENCE
    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<>();

    protected static String serviceKey(URL url) {
        return ProtocolUtils.serviceKey(url);
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

    public void destroy() {
        invokers.forEach(invoker -> {
            if (invoker != null) {
                invokers.remove(invoker);
                try {
                    LogHelper.info(logger, "Destroy reference: " + invoker.getUrl());
                    invoker.destroy();
                } catch (Throwable t) {
                    LogHelper.warn(logger, t.getMessage(), t);
                }
            }
        });
        exporterMap.keySet().forEach(key -> {
            Exporter<?> exporter = exporterMap.remove(key);
            if (exporter != null) {
                try {
                    LogHelper.info(logger, "Unexport service: " + exporter.getInvoker().getUrl());
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    protected static int getServerShutdownTimeout() {
        int timeout = Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        String value = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_KEY);
        if (!Strings.isNullOrEmpty(value)) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception ignore) {
            }
        } else {
            value = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
            if (!Strings.isNullOrEmpty(value)) {
                try {
                    timeout = Integer.parseInt(value) * 1000;
                } catch (Exception ignore) {
                }
            }
        }

        return timeout;
    }
}