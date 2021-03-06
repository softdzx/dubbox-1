package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.alibaba.dubbo.rpc.protocol.dubbo.filter.TraceFilter;
import com.google.common.base.Strings;

import java.lang.reflect.Method;

/**
 * TraceTelnetHandler
 *
 * @author william.liangf
 */
@Activate
@Help(parameter = "[service] [method] [times]", summary = "Trace the service.", detail = "Trace the service.")
public class TraceTelnetHandler implements TelnetHandler {

    public String telnet(Channel channel, String message) {
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
        if ((Strings.isNullOrEmpty(service)) && (Strings.isNullOrEmpty(message))) {
            return "Please input service name, eg: \r\ntrace XxxService\r\ntrace XxxService xxxMethod\r\ntrace XxxService xxxMethod 10\r\nor \"cd XxxService\" firstly.";
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
            if (method != null && method.length() > 0) {
                boolean found = false;
                for (Method m : invoker.getInterface().getMethods()) {
                    if (m.getName().equals(method)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "No such method " + method + " in class " + invoker.getInterface().getName();
                }
            }
            TraceFilter.addTracer(invoker.getInterface(), method, channel, Integer.parseInt(times));
        } else {
            return "No such service " + service;
        }
        return null;
    }

}