package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.google.common.base.Strings;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
abstract public class ProtocolUtils {

    public static String serviceKey(URL url) {
        return serviceKey(url.getRealPort(), url.getPath(), url.getParameter(Constants.VERSION_KEY),
                url.getParameter(Constants.GROUP_KEY));
    }

    public static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        StringBuilder buf = new StringBuilder();
        if (!Strings.isNullOrEmpty(serviceGroup)) {
            buf.append(serviceGroup);
            buf.append("/");
        }
        buf.append(serviceName);
        if (!Strings.isNullOrEmpty(serviceVersion) && !"0.0.0".equals(serviceVersion)) {
            buf.append(":");
            buf.append(serviceVersion);
        }
        buf.append(":");
        buf.append(port);
        return buf.toString();
    }

    public static boolean isGeneric(String generic) {
        return !Strings.isNullOrEmpty(generic)
                && (Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)  /* 正常的泛化调用 */
                || Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic) /* 支持java序列化的流式泛化调用 */
                || Constants.GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic));
    }

    public static boolean isDefaultGenericSerialization(String generic) {
        return isGeneric(generic)
                && Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic);
    }

    public static boolean isJavaGenericSerialization(String generic) {
        return isGeneric(generic)
                && Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic);
    }

    public static boolean isBeanGenericSerialization(String generic) {
        return isGeneric(generic) && Constants.GENERIC_SERIALIZATION_BEAN.equals(generic);
    }
}
