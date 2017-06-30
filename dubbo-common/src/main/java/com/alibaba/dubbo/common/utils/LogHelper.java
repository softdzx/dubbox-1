package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.logger.Logger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
abstract public class LogHelper {

    public static void trace(Logger logger, String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(msg);
        }
    }

    public static void trace(Logger logger, Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(throwable);
        }
    }

    public static void trace(Logger logger, String msg, Throwable e) {
        if (logger.isTraceEnabled()) {
            logger.trace(msg, e);
        }
    }

    public static void debug(Logger logger, String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg);
        }
    }

    public static void debug(Logger logger, Throwable e) {
        if (logger.isDebugEnabled()) {
            logger.debug(e);
        }
    }

    public static void debug(Logger logger, String msg, Throwable e) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg, e);
        }
    }

    public static void info(Logger logger, String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(msg);
        }
    }

    public static void info(Logger logger, Throwable e) {
        if (logger.isInfoEnabled()) {
            logger.info(e);
        }
    }

    public static void info(Logger logger, String msg, Throwable e) {
        if (logger.isInfoEnabled()) {
            logger.info(msg, e);
        }
    }

    public static void warn(Logger logger, String msg, Throwable e) {
        if (logger.isWarnEnabled()) {
            logger.warn(msg, e);
        }
    }

    public static void warn(Logger logger, String msg) {
        if (logger.isWarnEnabled()) {
            logger.warn(msg);
        }
    }

    public static void warn(Logger logger, Throwable e) {
        if (logger.isWarnEnabled()) {
            logger.warn(e);
        }
    }

    public static void error(Logger logger, Throwable e) {
        if (logger.isErrorEnabled()) {
            logger.error(e);
        }
    }

    public static void error(Logger logger, String msg) {
        if (logger.isErrorEnabled()) {
            logger.error(msg);
        }
    }

    public static void error(Logger logger, String msg, Throwable e) {
        if (logger.isErrorEnabled()) {
            logger.error(msg, e);
        }
    }

}
