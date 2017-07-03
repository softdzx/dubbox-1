package com.alibaba.dubbo.rpc.cluster.configurator;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractConfigurator implements Configurator {

    private final URL configuratorUrl;

    public AbstractConfigurator(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("configurator url == null");
        }
        this.configuratorUrl = url;
    }

    public URL getUrl() {
        return configuratorUrl;
    }

    public URL configure(URL url) {
        String host = configuratorUrl.getHost();
        if (host == null || url == null || url.getHost() == null) {
            return url;
        }
        if (Constants.ANYHOST_VALUE.equals(host) || url.getHost().equals(host)) {
            String configApplication = configuratorUrl.getParameter(Constants.APPLICATION_KEY, configuratorUrl.getUsername());
            String currentApplication = url.getParameter(Constants.APPLICATION_KEY, url.getUsername());
            if (configApplication == null || Constants.ANY_VALUE.equals(configApplication)
                    || configApplication.equals(currentApplication)) {
                int port = configuratorUrl.getPort();
                if (port == 0 || url.getPort() == port) {
                    Set<String> condtionKeys = Sets.newHashSet();
                    condtionKeys.add(Constants.CATEGORY_KEY);
                    condtionKeys.add(Constants.CHECK_KEY);
                    condtionKeys.add(Constants.DYNAMIC_KEY);
                    condtionKeys.add(Constants.ENABLED_KEY);
                    for (Map.Entry<String, String> entry : configuratorUrl.getParameters().entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (key.startsWith("~") || Constants.APPLICATION_KEY.equals(key)
                                || Constants.SIDE_KEY.equals(key)) {
                            condtionKeys.add(key);
                            if (value != null && !Constants.ANY_VALUE.equals(value)
                                    && !value.equals(url.getParameter(key.startsWith("~") ? key.substring(1) : key))) {
                                return url;
                            }
                        }
                    }
                    return doConfigure(url, configuratorUrl.removeParameters(condtionKeys));
                }
            }
        }
        return url;
    }

    public int compareTo(Configurator o) {
        if (o == null) {
            return -1;
        }
        return getUrl().getHost().compareTo(o.getUrl().getHost());
    }

    protected abstract URL doConfigure(URL currentUrl, URL configUrl);

    public static void main(String[] args) {
        System.out.println(URL.encode("timeout=100"));
    }

}
