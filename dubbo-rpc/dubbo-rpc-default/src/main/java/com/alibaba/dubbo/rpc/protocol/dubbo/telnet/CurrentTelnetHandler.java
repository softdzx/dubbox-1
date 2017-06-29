package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.google.common.base.Strings;

/**
 * CurrentServiceTelnetHandler
 *
 * @author william.liangf
 */
@Activate
@Help(summary = "Print working default service.", detail = "Print working default service.")
public class CurrentTelnetHandler implements TelnetHandler {

    public String telnet(Channel channel, String message) {
        if (message.length() > 0) {
            return "Unsupported parameter " + message + " for pwd.";
        }
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
        StringBuilder buf = new StringBuilder();
        if (Strings.isNullOrEmpty(service)) {
            buf.append("/");
        } else {
            buf.append(service);
        }
        return buf.toString();
    }

}