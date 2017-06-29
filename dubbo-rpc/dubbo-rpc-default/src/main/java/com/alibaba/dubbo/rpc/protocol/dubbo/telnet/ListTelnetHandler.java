package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.google.common.base.Strings;

import java.lang.reflect.Method;

/**
 * ListTelnetHandler
 *
 * @author william.liangf
 */
@Activate
@Help(parameter = "[-l] [service]", summary = "List services and methods.", detail = "List services and methods.")
public class ListTelnetHandler implements TelnetHandler {

    public String telnet(Channel channel, String message) {
        StringBuilder buf = new StringBuilder();
        String service = null;
        boolean detail = false;
        if (!Strings.isNullOrEmpty(message)) {
            String[] parts = message.split("\\s+");
            for (String part : parts) {
                if ("-l".equals(part)) {
                    detail = true;
                } else {
                    if (!Strings.isNullOrEmpty(service)) {
                        return "Invaild parameter " + part;
                    }
                    service = part;
                }
            }
        } else {
            service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
            if (!Strings.isNullOrEmpty(service)) {
                buf.append("Use default service " + service + ".\r\n");
            }
        }
        if (Strings.isNullOrEmpty(service)) {
            for (Exporter<?> exporter : DubboProtocol.getDubboProtocol().getExporters()) {
                if (buf.length() > 0) {
                    buf.append("\r\n");
                }
                buf.append(exporter.getInvoker().getInterface().getName());
                if (detail) {
                    buf.append(" -> ");
                    buf.append(exporter.getInvoker().getUrl());
                }
            }
        } else {
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
                Method[] methods = invoker.getInterface().getMethods();
                for (Method method : methods) {
                    if (buf.length() > 0) {
                        buf.append("\r\n");
                    }
                    if (detail) {
                        buf.append(ReflectUtils.getName(method));
                    } else {
                        buf.append(method.getName());
                    }
                }
            } else {
                buf.append("No such service " + service);
            }
        }
        return buf.toString();
    }

}