package com.alibaba.dubbo.rpc.protocol.dubbo.page;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * ClientsPageHandler
 *
 * @author william.liangf
 */
public class ClientsPageHandler implements PageHandler {

    public Page handle(URL url) {
        String port = url.getParameter("port");
        int p = Strings.isNullOrEmpty(port) ? 0 : Integer.parseInt(port);
        Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
        ExchangeServer server = null;
        StringBuilder select = new StringBuilder();
        if (!CollectionUtils.isEmpty(servers)) {
            if (servers.size() == 1) {
                server = servers.iterator().next();
                String address = server.getUrl().getAddress();
                select.append(" &gt; " + NetUtils.getHostName(address) + "/" + address);
            } else {
                select.append(" &gt; <select onchange=\"window.location.href='clients.html?port=' + this.value;\">");
                for (ExchangeServer s : servers) {
                    int sp = s.getUrl().getPort();
                    select.append("<option value=\">");
                    select.append(sp);
                    if (p == 0 && server == null || p == sp) {
                        server = s;
                        select.append("\" selected=\"selected");
                    }
                    select.append("\">");
                    select.append(s.getUrl().getAddress());
                    select.append("</option>");
                }
                select.append("</select>");
            }
        }
        List<List<String>> rows = Lists.newArrayList();
        if (server != null) {
            Collection<ExchangeChannel> channels = server.getExchangeChannels();
            for (ExchangeChannel c : channels) {
                List<String> row = Lists.newArrayList();
                String address = NetUtils.toAddressString(c.getRemoteAddress());
                row.add(NetUtils.getHostName(address) + "/" + address);
                rows.add(row);
            }
        }
        return new Page("<a href=\"servers.html\">Servers</a>" + select.toString() + " &gt; Clients",
                "Clients (" + rows.size() + ")", new String[]{"Client Address:"}, rows);
    }

}