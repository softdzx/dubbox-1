package com.alibaba.dubbo.governance.web.common.pulltool;

import com.google.common.base.Strings;

public class RootContextPath {

    private String contextPath;

    public RootContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getURI(String uri) {
        String prefix = !Strings.isNullOrEmpty(contextPath) && !"/".equals(contextPath) ? contextPath : "";
        return uri.startsWith("/") ? prefix + uri : prefix + "/" + uri;
    }

}
