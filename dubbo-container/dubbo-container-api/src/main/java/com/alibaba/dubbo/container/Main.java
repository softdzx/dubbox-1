package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.google.common.collect.Lists;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Main. (API, Static, ThreadSafe)
 *
 * @author william.liangf
 */
public class Main {

    public static final String CONTAINER_KEY = "dubbo.container";

    public static final String SHUTDOWN_HOOK_KEY = "dubbo.shutdown.hook";

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final ExtensionLoader<Container> loader = ExtensionLoader.getExtensionLoader(Container.class);

    private static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            if (CollectionUtils.isEmpty(args)) {
                String config = ConfigUtils.getProperty(CONTAINER_KEY, loader.getDefaultExtensionName());
                args = Constants.COMMA_SPLIT_PATTERN.split(config);
            }

            final List<Container> containers = Lists.newArrayList();
            for (String arg : args) {
                containers.add(loader.getExtension(arg));
            }
            logger.info("Use container type(" + Arrays.toString(args) + ") to run dubbo serivce.");

            if ("true".equals(System.getProperty(SHUTDOWN_HOOK_KEY))) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    for (Container container : containers) {
                        try {
                            container.stop();
                            LogHelper.info(logger, "Dubbo " + container.getClass().getSimpleName() + " stopped!");
                        } catch (Throwable t) {
                            LogHelper.error(logger, t.getMessage(), t);
                        }
                        synchronized (Main.class) {
                            running = false;
                            Main.class.notify();
                        }
                    }
                }));
            }

            for (Container container : containers) {
                container.start();
                LogHelper.info(logger, "Dubbo " + container.getClass().getSimpleName() + " started!");
            }
            System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date()) + " Dubbo service server started!");
        } catch (RuntimeException e) {
            e.printStackTrace();
            LogHelper.error(logger, e.getMessage(), e);
            System.exit(1);
        }
        synchronized (Main.class) {
            while (running) {
                try {
                    Main.class.wait();
                } catch (Throwable ignore) {
                }
            }
        }
    }

}