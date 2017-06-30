package com.alibaba.dubbo.config.spring.status;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;

import java.lang.reflect.Method;

/**
 * SpringStatusChecker
 *
 * @author william.liangf
 */
@Activate
public class SpringStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(SpringStatusChecker.class);

    public Status check() {
        ApplicationContext context = ServiceBean.getSpringContext();
        if (context == null) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level;
        if (context instanceof Lifecycle) {
            level = ((Lifecycle) context).isRunning() ? Status.Level.OK : Status.Level.ERROR;
        } else {
            level = Status.Level.UNKNOWN;
        }
        StringBuilder buf = new StringBuilder();
        try {
            Class<?> cls = context.getClass();
            Method method = null;
            while (cls != null && method == null) {
                try {
                    method = cls.getDeclaredMethod("getConfigLocations");
                } catch (NoSuchMethodException t) {
                    cls = cls.getSuperclass();
                }
            }
            if (method != null) {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                String[] configs = (String[]) method.invoke(context);
                if (!CollectionUtils.isEmpty(configs)) {
                    for (String config : configs) {
                        if (buf.length() > 0) {
                            buf.append(",");
                        }
                        buf.append(config);
                    }
                }
            }
        } catch (Throwable t) {
            LogHelper.warn(logger, t.getMessage(), t);
        }
        return new Status(level, buf.toString());
    }

}