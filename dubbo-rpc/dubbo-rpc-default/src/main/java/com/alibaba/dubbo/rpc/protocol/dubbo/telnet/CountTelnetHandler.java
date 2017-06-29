/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.remoting.telnet.support.TelnetUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcStatus;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * CountTelnetHandler
 *
 * @author william.liangf
 */
@Activate
@Help(parameter = "[service] [method] [times]", summary = "Count the service.", detail = "Count the service.")
public class CountTelnetHandler implements TelnetHandler {

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public String telnet(final Channel channel, String message) {
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
        if ((Strings.isNullOrEmpty(service)) && (Strings.isNullOrEmpty(message))) {
            return "Please input service name, eg: \r\ncount XxxService\r\ncount XxxService xxxMethod\r\ncount XxxService xxxMethod 10\r\nor \"cd XxxService\" firstly.";
        }
        StringBuilder buf = new StringBuilder();
        if (!Strings.isNullOrEmpty(service)) {
            buf.append("Use default service " + service + ".\r\n");
        }
        String[] parts = message.split("\\s+");
        String method;
        String times;
        if (Strings.isNullOrEmpty(service)) {
            service = parts.length > 0 ? parts[0] : null;
            method = parts.length > 1 ? parts[1] : null;
        } else {
            method = parts.length > 0 ? parts[0] : null;
        }
        if (StringUtils.isInteger(method)) {
            times = method;
            method = null;
        } else {
            times = parts.length > 2 ? parts[2] : "1";
        }
        if (!StringUtils.isInteger(times)) {
            return "Illegal times " + times + ", must be integer.";
        }
        final int t = Integer.parseInt(times);
        Invoker<?> invoker = null;
        for (Exporter<?> exporter : DubboProtocol.getDubboProtocol().getExporters()) {
            Invoker<?> invoker1 = exporter.getInvoker();
            if (service.equals(invoker1.getInterface().getSimpleName())
                    || service.equals(invoker1.getInterface().getName())
                    || service.equals(invoker1.getUrl().getPath())) {
                invoker = invoker1;
                break;
            }
        }
        if (invoker != null) {
            if (t > 0) {
                final String mtd = method;
                final Invoker<?> inv = invoker;
                final String prompt = channel.getUrl().getParameter("prompt", "telnet");
                Thread thread = new Thread(() -> {
                    for (int i = 0; i < t; i++) {
                        String result = count(inv, mtd);
                        try {
                            channel.send("\r\n" + result);
                        } catch (RemotingException e1) {
                            return;
                        }
                        if (i < t - 1) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }
                    try {
                        channel.send("\r\n" + prompt + "> ");
                    } catch (RemotingException ignore) {
                    }
                }, "TelnetCount");
                thread.setDaemon(true);
                thread.start();
            }
        } else {
            buf.append("No such service " + service);
        }
        return buf.toString();
    }

    private String count(Invoker<?> invoker, String method) {
        URL url = invoker.getUrl();
        List<List<String>> table = Lists.newArrayList();
        List<String> header = Lists.newArrayList();
        header.add("method");
        header.add("total");
        header.add("failed");
        header.add("active");
        header.add("average");
        header.add("max");
        if (Strings.isNullOrEmpty(method)) {
            for (Method m : invoker.getInterface().getMethods()) {
                RpcStatus count = RpcStatus.getStatus(url, m.getName());
                List<String> row = new ArrayList<>();
                row.add(m.getName());
                row.add(String.valueOf(count.getTotal()));
                row.add(String.valueOf(count.getFailed()));
                row.add(String.valueOf(count.getActive()));
                row.add(String.valueOf(count.getSucceededAverageElapsed()) + "ms");
                row.add(String.valueOf(count.getSucceededMaxElapsed()) + "ms");
                table.add(row);
            }
        } else {
            boolean found = false;
            for (Method m : invoker.getInterface().getMethods()) {
                if (m.getName().equals(method)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                RpcStatus count = RpcStatus.getStatus(url, method);
                List<String> row = Lists.newArrayList();
                row.add(method);
                row.add(String.valueOf(count.getTotal()));
                row.add(String.valueOf(count.getFailed()));
                row.add(String.valueOf(count.getActive()));
                row.add(String.valueOf(count.getSucceededAverageElapsed()) + "ms");
                row.add(String.valueOf(count.getSucceededMaxElapsed()) + "ms");
                table.add(row);
            } else {
                return "No such method " + method + " in class " + invoker.getInterface().getName();
            }
        }
        return TelnetUtils.toTable(header, table);
    }

}