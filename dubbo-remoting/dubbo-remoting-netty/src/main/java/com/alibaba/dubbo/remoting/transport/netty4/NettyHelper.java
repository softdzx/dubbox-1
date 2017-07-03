package com.alibaba.dubbo.remoting.transport.netty4;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

final class NettyHelper {

    public static void setNettyLoggerFactory() {
        InternalLoggerFactory factory = InternalLoggerFactory.getDefaultFactory();
        if (factory == null || !(factory instanceof DubboLoggerFactory)) {
            InternalLoggerFactory.setDefaultFactory(new DubboLoggerFactory());
        }
    }

    static class DubboLoggerFactory extends InternalLoggerFactory {

        @Override
        public InternalLogger newInstance(String name) {
            return new DubboLogger(LoggerFactory.getLogger(name));
        }
    }

    static class DubboLogger extends AbstractInternalLogger {

        private Logger logger;

        DubboLogger(Logger logger) {
            super(logger.toString());
            this.logger = logger;
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            logger.trace(msg);
        }

        @Override
        public void trace(String msg, Object o) {
        }

        @Override
        public void trace(String msg, Object o, Object o2) {
        }

        @Override
        public void trace(String msg, Object... objects) {
        }

        @Override
        public void trace(String msg, Throwable throwable) {
            logger.trace(msg, throwable);
        }

        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        public void debug(String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(String msg, Object o) {
        }

        @Override
        public void debug(String msg, Object o, Object o2) {
        }

        @Override
        public void debug(String msg, Object... objects) {
        }

        public void debug(String msg, Throwable cause) {
            logger.debug(msg, cause);
        }

        public void info(String msg) {
            logger.info(msg);
        }

        @Override
        public void info(String msg, Object o) {
        }

        @Override
        public void info(String msg, Object o, Object o2) {
        }

        @Override
        public void info(String msg, Object... objects) {
        }

        public void info(String msg, Throwable cause) {
            logger.info(msg, cause);
        }

        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void warn(String s, Object o) {
        }

        @Override
        public void warn(String s, Object... objects) {
        }

        @Override
        public void warn(String s, Object o, Object o2) {
        }

        public void warn(String msg, Throwable cause) {
            logger.warn(msg, cause);
        }

        public void error(String msg) {
            logger.error(msg);
        }

        @Override
        public void error(String msg, Object o) {
        }

        @Override
        public void error(String msg, Object o, Object o2) {
        }

        @Override
        public void error(String msg, Object... objects) {
        }

        public void error(String msg, Throwable cause) {
            logger.error(msg, cause);
        }

        @Override
        public String toString() {
            return logger.toString();
        }
    }

}
