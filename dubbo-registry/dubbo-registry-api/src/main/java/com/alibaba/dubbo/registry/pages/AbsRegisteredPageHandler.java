package com.alibaba.dubbo.registry.pages;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.support.AbstractRegistry;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by wugy on 2017/7/1 09:34
 */
abstract public class AbsRegisteredPageHandler implements PageHandler {

    @Override
    public Page handle(URL url) {
        String registryAddress = url.getParameter("registry", "");
        List<List<String>> rows = Lists.newArrayList();
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        StringBuilder select = new StringBuilder();
        Registry registry = null;
        if (!CollectionUtils.isEmpty(registries)) {
            if (registries.size() == 1) {
                registry = registries.iterator().next();
                select.append(" &gt; " + registry.getUrl().getAddress());
            } else {
                select.append(" &gt; <select onchange=\"window.location.href='" + getHtmlUrl() + "?registry=' + this.value;\">");
                for (Registry r : registries) {
                    String sp = r.getUrl().getAddress();
                    select.append("<option value=\">");
                    select.append(sp);
                    if (Strings.isNullOrEmpty(registryAddress) && registry == null
                            || registryAddress.equals(sp)) {
                        registry = r;
                        select.append("\" selected=\"selected");
                    }
                    select.append("\">");
                    select.append(sp);
                    select.append("</option>");
                }
                select.append("</select>");
            }
        }
        if (registry instanceof AbstractRegistry) {
            Set<URL> services = ((AbstractRegistry) registry).getRegistered();
            if (!CollectionUtils.isEmpty(services)) {
                for (URL u : services) {
                    List<String> row = Lists.newArrayList();
                    row.add(u.toFullString().replace("<", "&lt;").replace(">", "&gt;"));
                    rows.add(row);
                }
            }
        }
        return getPage(select.toString(), rows, registryAddress);
    }

    abstract public Page getPage(String html, List<List<String>> rows, String registryAddress);

    abstract public String getHtmlUrl();
}
