/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.consul;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * consul registry
 *
 * @author linux_china
 */
public class ConsulRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);
    ConsulClient consulClient;

    private static final int DEFAULT_CONSUL_PORT = 8500;

    private final ConcurrentMap<String, NotifyListenerConsulWrapper> notifiers = new ConcurrentHashMap<>();

    public ConsulRegistry(URL url) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        consulClient = new ConsulClient(url.getHost(), url.getPort());
    }

    @Override
    public boolean isAvailable() {
        return !consulClient.getDatacenters().getValue().isEmpty();
    }

    @Override
    protected void doRegister(URL url) {
        NewService consulService = new NewService();
        consulService.setAddress(url.toFullString());
        consulService.setPort(url.getPort());
        consulService.setId(convertConsulSerivceId(url));
        consulService.setName(url.getServiceInterface());
        consulClient.agentServiceRegister(consulService);
    }

    @Override
    protected void doUnregister(URL url) {
        consulClient.agentServiceDeregister(convertConsulSerivceId(url));
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        String serviceName = url.getServiceInterface();
        NotifyListenerConsulWrapper wrapper = new NotifyListenerConsulWrapper(listener, consulClient, serviceName);
        if (notifiers.isEmpty()) {
            ServiceLookupThread lookupThread = new ServiceLookupThread();
            lookupThread.setDaemon(true);
            lookupThread.start();
        }
        if (!notifiers.containsKey(serviceName)) {
            notifiers.put(serviceName, wrapper);
            wrapper.sync();
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        notifiers.remove(url.getServiceInterface());
    }

    /**
     * 根据motan的url生成consul的serivce id。 serviceid 包括ip＋port＋rpc服务的接口类名
     *
     * @param url url
     * @return consul service id
     */
    public static String convertConsulSerivceId(URL url) {
        if (url == null) {
            return null;
        }
        return convertServiceId(url.getHost(), url.getPort(), url.getPath());
    }

    public static String convertServiceId(String host, int port, String path) {
        return host + ":" + port + "-" + path;
    }

    private class ServiceLookupThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    sleep(15000);
                    synchronized (notifiers) {
                        for (NotifyListenerConsulWrapper wrapper : notifiers.values()) {
                            wrapper.sync();
                        }
                    }
                } catch (Throwable e) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
