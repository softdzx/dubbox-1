package com.alibaba.dubbo.container.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.container.Container;
import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;

/**
 * LogbackContainer. (SPI, Singleton, ThreadSafe)
 *
 * @author {@link "mailto:qq55355383@gmail.com" "yi.tong"}
 */
public class LogbackContainer implements Container {

    private static final String LOGBACK_FILE = "dubbo.logback.file";

    private static final String LOGBACK_LEVEL = "dubbo.logback.level";

    private static final String LOGBACK_MAX_HISTORY = "dubbo.logback.maxhistory";

    private static final String DEFAULT_LOGBACK_LEVEL = "ERROR";

    public void start() {
        String file = ConfigUtils.getProperty(LOGBACK_FILE);
        if (!Strings.isNullOrEmpty(file)) {
            String level = ConfigUtils.getProperty(LOGBACK_LEVEL);
            if (Strings.isNullOrEmpty(level)) {
                level = DEFAULT_LOGBACK_LEVEL;
            }
            // maxHistory=0 Infinite history
            int maxHistory = StringUtils.parseInteger(ConfigUtils.getProperty(LOGBACK_MAX_HISTORY));

            doInitializer(file, level, maxHistory);
        }
    }

    public void stop() {
    }

    /**
     * Initializer logback
     *
     * @param file       log file
     * @param level      log level
     * @param maxHistory max history
     */
    private void doInitializer(String file, String level, int maxHistory) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();

        // appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("application");
        fileAppender.setFile(file);
        fileAppender.setAppend(true);

        // policy
        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(loggerContext);
        policy.setMaxHistory(maxHistory);
        policy.setFileNamePattern(file + ".%d{yyyy-MM-dd}");
        policy.setParent(fileAppender);
        policy.start();
        fileAppender.setRollingPolicy(policy);

        // encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%date [%thread] %-5level %logger (%file:%line\\) - %msg%n");
        encoder.start();
        fileAppender.setEncoder(encoder);

        fileAppender.start();

        rootLogger.addAppender(fileAppender);
        rootLogger.setLevel(Level.toLevel(level));
        rootLogger.setAdditive(false);
    }

}