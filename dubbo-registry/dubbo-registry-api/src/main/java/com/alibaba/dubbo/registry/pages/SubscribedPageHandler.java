package com.alibaba.dubbo.registry.pages;

import com.alibaba.dubbo.container.page.Page;

import java.util.List;

/**
 * SubscribedPageHandler
 *
 * @author william.liangf
 */
public class SubscribedPageHandler extends AbsRegisteredPageHandler {


    @Override
    public Page getPage(String html, List<List<String>> rows, String registryAddress) {
        return new Page("<a href=\"registries.html\">Registries</a>" + html +
                " &gt; <a href=\"registered.html?registry=" + registryAddress + "\">Registered</a> | Subscribed",
                "Subscribed (" + rows.size() + ")", new String[]{"Consumer URL:"}, rows);
    }

    @Override
    public String getHtmlUrl() {
        return "subscribed.html";
    }

}