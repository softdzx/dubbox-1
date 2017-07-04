package com.alibaba.dubbo.governance.web.common.module.screen;

import java.util.Map;

import com.alibaba.dubbo.governance.web.common.pulltool.RootContextPath;

public class Error_other {
	
	public void execute(Map<String, Object> context) throws Throwable {
		String contextPath = (String) context.get("request.contextPath");
        context.put("rootContextPath", new RootContextPath(contextPath));
	}

}
