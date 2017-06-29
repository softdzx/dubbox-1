package com.alibaba.dubbo.rpc.protocol.dubbo.page;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ServersPageHandler
 *
 * @author william.liangf
 */
@Menu(name = "Servers", desc = "Show exported service servers.", order = 14000)
public class ServersPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = Lists.newArrayList();
        Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
        int clientCount = 0;
        if (!CollectionUtils.isEmpty(servers)) {
            for (ExchangeServer s : servers) {
                List<String> row = new ArrayList<>();
                String address = s.getUrl().getAddress();
                row.add(NetUtils.getHostName(address) + "/" + address);
                int clientSize = s.getExchangeChannels().size();
                clientCount += clientSize;
                row.add("<a href=\"clients.html?port=" + s.getUrl().getPort() + "\">Clients(" + clientSize + ")</a>");
                rows.add(row);
            }
        }
        return new Page("Servers", "Servers (" + rows.size() + ")", new String[]{"Server Address:",
                "Clients(" + clientCount + ")"}, rows);
    }

}