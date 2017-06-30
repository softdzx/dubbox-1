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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Map;

/**
 * DataSourceStatusChecker
 *
 * @author william.liangf
 */
@Activate
public class DataSourceStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceStatusChecker.class);

    @SuppressWarnings("unchecked")
    public Status check() {
        ApplicationContext context = ServiceBean.getSpringContext();
        if (context == null) {
            return new Status(Status.Level.UNKNOWN);
        }
        Map<String, DataSource> dataSources = context.getBeansOfType(DataSource.class, false, false);
        if (CollectionUtils.isEmpty(dataSources)) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            DataSource dataSource = entry.getValue();
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(entry.getKey());
            try {
                try (Connection connection = dataSource.getConnection()) {
                    DatabaseMetaData metaData = connection.getMetaData();
                    try (ResultSet resultSet = metaData.getTypeInfo()) {
                        if (!resultSet.next()) {
                            level = Status.Level.ERROR;
                        }
                    }
                    buf.append(metaData.getURL());
                    buf.append("(");
                    buf.append(metaData.getDatabaseProductName());
                    buf.append("-");
                    buf.append(metaData.getDatabaseProductVersion());
                    buf.append(")");
                }
            } catch (Throwable e) {
                LogHelper.warn(logger, e.getMessage(), e);
                return new Status(level, e.getMessage());
            }
        }
        return new Status(level, buf.toString());
    }

}