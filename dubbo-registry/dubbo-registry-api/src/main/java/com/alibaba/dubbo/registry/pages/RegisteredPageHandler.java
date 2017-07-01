package com.alibaba.dubbo.registry.pages;

import com.alibaba.dubbo.container.page.Page;

import java.util.List;

/**
 * RegisteredPageHandler
 *
 * @author william.liangf
 */
public class RegisteredPageHandler extends AbsRegisteredPageHandler {

    @Override
    public Page getPage(String html, List<List<String>> rows, String registryAddress) {
        return new Page("<a href=\"registries.html\">Registries</a>" + html +
                " &gt; Registered | <a href=\"subscribed.html?registry=" + registryAddress + "\">Subscribed</a>",
                "Registered (" + rows.size() + ")", new String[]{"Provider URL:"}, rows);
    }

    @Override
    public String getHtmlUrl() {
        return "registered.html";
    }
}