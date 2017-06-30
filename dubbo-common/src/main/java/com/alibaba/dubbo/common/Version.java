package com.alibaba.dubbo.common;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.Set;

/**
 * Version
 *
 * @author william.liangf
 */
public final class Version {

    private Version() {
    }

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    private static final String VERSION = getVersion(Version.class, "3.0.0");

    static {
        // 检查是否存在重复的jar包
        Version.checkDuplicate(Version.class);
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getVersion(Class<?> cls, String defaultVersion) {
        try {
            // 首先查找MANIFEST.MF规范中的版本号
            String version = cls.getPackage().getImplementationVersion();
            if (Strings.isNullOrEmpty(version)) {
                version = cls.getPackage().getSpecificationVersion();
            }
            if (Strings.isNullOrEmpty(version)) {
                // 如果规范中没有版本号，基于jar包名获取版本号
                CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
                if (codeSource == null) {
                    logger.info("No codeSource for class " + cls.getName() + " when getVersion, use default version " + defaultVersion);
                } else {
                    String file = codeSource.getLocation().getFile();
                    if (!Strings.isNullOrEmpty(file) && file.endsWith(".jar")) {
                        file = file.substring(0, file.length() - 4);
                        int i = file.lastIndexOf('/');
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        i = file.indexOf("-");
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        while (file.length() > 0 && !Character.isDigit(file.charAt(0))) {
                            i = file.indexOf("-");
                            if (i >= 0) {
                                file = file.substring(i + 1);
                            } else {
                                break;
                            }
                        }
                        version = file;
                    }
                }
            }
            // 返回版本号，如果为空返回缺省版本号
            return Strings.isNullOrEmpty(version) ? defaultVersion : version;
        } catch (Throwable e) { // 防御性容错
            // 忽略异常，返回缺省版本号
            LogHelper.error(logger, "return default version, ignore exception " + e.getMessage(), e);
            return defaultVersion;
        }
    }

    public static void checkDuplicate(Class<?> cls, boolean failOnError) {
        checkDuplicate(cls.getName().replace('.', '/') + ".class", failOnError);
    }

    public static void checkDuplicate(Class<?> cls) {
        checkDuplicate(cls, false);
    }

    public static void checkDuplicate(String path, boolean failOnError) {
        try {
            // 在ClassPath搜文件
            Enumeration<URL> urls = ClassHelper.getCallerClassLoader(Version.class).getResources(path);
            Set<String> files = Sets.newHashSet();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String file = url.getFile();
                    if (!Strings.isNullOrEmpty(file)) {
                        files.add(file);
                    }
                }
            }
            // 如果有多个，就表示重复
            if (files.size() > 1) {
                String error = "Duplicate class " + path + " in " + files.size() + " jar " + files;
                if (failOnError) {
                    throw new IllegalStateException(error);
                } else {
                    LogHelper.error(logger, error);
                }
            }
        } catch (Throwable e) { // 防御性容错
            LogHelper.error(logger, e.getMessage(), e);
        }
    }

}