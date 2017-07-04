package com.alibaba.dubbo.governance.web.common.pulltool;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.citrus.service.pull.ToolFactory;
import com.alibaba.dubbo.governance.biz.common.i18n.MessageResourceService;

/**
 * PullTool for accessing message bundle.
 * 
 * @author gerry
 */
public class I18nMessageTool implements ToolFactory {

    @Autowired
    private MessageResourceService messageResourceService;

    public Object createTool() throws Exception {
        return messageResourceService;
    }

    private boolean singleton = true; //应该是global范围的对象！！

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }
    
    public boolean isSingleton() {
        return this.singleton;
    }

}
