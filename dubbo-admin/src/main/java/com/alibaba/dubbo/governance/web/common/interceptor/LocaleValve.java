package com.alibaba.dubbo.governance.web.common.interceptor;

import com.alibaba.citrus.service.pipeline.PipelineContext;
import com.alibaba.citrus.service.pipeline.support.AbstractValve;
import com.alibaba.citrus.turbine.TurbineRunData;
import com.alibaba.dubbo.governance.web.common.i18n.LocaleUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Set;

import static com.alibaba.citrus.turbine.util.TurbineUtil.getTurbineRunData;

public class LocaleValve extends AbstractValve {

    @Autowired
    private HttpServletRequest request;

    //添加拦截器例外设置
    private final static Set<String> TARGET_WITHOUT_CHECK = Sets.newHashSet();

    static {
        TARGET_WITHOUT_CHECK.add("/ok");
        TARGET_WITHOUT_CHECK.add("/error");
        TARGET_WITHOUT_CHECK.add("/login");
        TARGET_WITHOUT_CHECK.add("/logout");
    }

    private boolean ignoreTarget(String target) {
        return TARGET_WITHOUT_CHECK.contains(target);
    }

    @Override
    protected void init() throws Exception {
    }

    public void invoke(PipelineContext pipelineContext) throws Exception {
        TurbineRunData rundata = getTurbineRunData(request);
        if (ignoreTarget(rundata.getTarget())) {
            pipelineContext.invokeNext();
            return;
        }

        //默认是中文
        String[] temp = rundata.getCookies().getStrings("locale");
        String locale = null;
        if (temp != null) {
            if (temp.length > 1) {
                locale = temp[temp.length - 1];
            } else if (temp.length == 1) {
                locale = temp[0];
            }
        }
        if (Strings.isNullOrEmpty(locale)) {
            locale = "zh";
        }

        Locale newLocale = Locale.SIMPLIFIED_CHINESE;
        if ("en".equals(locale)) {
            newLocale = Locale.ENGLISH;
        } else if ("zh".equals(locale)) {
            newLocale = Locale.SIMPLIFIED_CHINESE;
        } else if ("zh_TW".equals(locale)) {
            newLocale = Locale.TRADITIONAL_CHINESE;
        }
        LocaleUtil.setLocale(newLocale);

        pipelineContext.invokeNext();
    }
}
