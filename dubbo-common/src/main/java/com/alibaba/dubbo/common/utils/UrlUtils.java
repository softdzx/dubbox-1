package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UrlUtils {

    public static URL parseURL(String address, Map<String, String> defaults) {
        if (Strings.isNullOrEmpty(address)) {
            return null;
        }
        String url;
        if (address.contains("://")) {
            url = address;
        } else {
            String[] addresses = Constants.COMMA_SPLIT_PATTERN.split(address);
            url = addresses[0];
            if (addresses.length > 1) {
                StringBuilder backup = new StringBuilder();
                for (int i = 1; i < addresses.length; i++) {
                    if (i > 1) {
                        backup.append(",");
                    }
                    backup.append(addresses[i]);
                }
                url += "?" + Constants.BACKUP_KEY + "=" + backup.toString();
            }
        }
        String defaultProtocol = defaults == null ? null : defaults.get("protocol");
        if (Strings.isNullOrEmpty(defaultProtocol)) {
            defaultProtocol = "dubbo";
        }
        String defaultUsername = defaults == null ? null : defaults.get("username");
        String defaultPassword = defaults == null ? null : defaults.get("password");
        int defaultPort = StringUtils.parseInteger(defaults == null ? null : defaults.get("port"));
        String defaultPath = defaults == null ? null : defaults.get("path");
        Map<String, String> defaultParameters = defaults == null ? null : Maps.newHashMap(defaults);
        if (defaultParameters != null) {
            defaultParameters.remove("protocol");
            defaultParameters.remove("username");
            defaultParameters.remove("password");
            defaultParameters.remove("host");
            defaultParameters.remove("port");
            defaultParameters.remove("path");
        }
        URL u = URL.valueOf(url);
        boolean changed = false;
        String protocol = u.getProtocol();
        String username = u.getUsername();
        String password = u.getPassword();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        Map<String, String> parameters = Maps.newHashMap(u.getParameters());
        if (Strings.isNullOrEmpty(protocol) && defaultProtocol.length() > 0) {
            changed = true;
            protocol = defaultProtocol;
        }
        if (Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(defaultUsername)) {
            changed = true;
            username = defaultUsername;
        }
        if (Strings.isNullOrEmpty(password) && !Strings.isNullOrEmpty(defaultPassword)) {
            changed = true;
            password = defaultPassword;
        }
        if (port <= 0) {
            if (defaultPort > 0) {
                changed = true;
                port = defaultPort;
            } else {
                changed = true;
                port = 9090;
            }
        }
        if (Strings.isNullOrEmpty(path)) {
            if (!Strings.isNullOrEmpty(defaultPath)) {
                changed = true;
                path = defaultPath;
            }
        }
        if (!CollectionUtils.isEmpty(defaultParameters)) {
            for (Map.Entry<String, String> entry : defaultParameters.entrySet()) {
                String key = entry.getKey();
                String defaultValue = entry.getValue();
                if (!Strings.isNullOrEmpty(defaultValue)) {
                    String value = parameters.get(key);
                    if (Strings.isNullOrEmpty(value)) {
                        changed = true;
                        parameters.put(key, defaultValue);
                    }
                }
            }
        }
        if (changed) {
            u = new URL(protocol, username, password, host, port, path, parameters);
        }
        return u;
    }

    public static List<URL> parseURLs(String address, Map<String, String> defaults) {
        if (Strings.isNullOrEmpty(address)) {
            return null;
        }
        String[] addresses = Constants.REGISTRY_SPLIT_PATTERN.split(address);
        if (CollectionUtils.isEmpty(addresses)) {
            return null; //here won't be empty
        }
        List<URL> registries = Lists.newArrayList();
        for (String addr : addresses) {
            registries.add(parseURL(addr, defaults));
        }
        return registries;
    }

    public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
        return buildMap(register);
    }

    public static Map<String, String> convertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = Maps.newHashMap();
        subscribe.forEach((serviceName, serviceQuery) -> {
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String group = params.get("group");
                String version = params.get("version");
                //params.remove("group");
                //params.remove("version");
                String name = !Strings.isNullOrEmpty(group) ? group + "/" + serviceName : serviceName;
                name = !Strings.isNullOrEmpty(version) ? name + ":" + version : name;
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        });
        return newSubscribe;
    }

    public static Map<String, Map<String, String>> revertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = Maps.newHashMap();
        register.forEach((serviceName, serviceUrls) -> {
            if (serviceName.contains(":") || serviceName.contains("/")) {
                serviceUrls.forEach((serviceUrl, serviceQuery) -> {
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String name = serviceName;
                    int i = name.indexOf('/');
                    if (i >= 0) {
                        params.put("group", name.substring(0, i));
                        name = name.substring(i + 1);
                    }
                    i = name.lastIndexOf(':');
                    if (i >= 0) {
                        params.put("version", name.substring(i + 1));
                        name = name.substring(0, i);
                    }
                    Map<String, String> newUrls = newRegister.computeIfAbsent(name, k -> Maps.newHashMap());
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                });
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        });
        return newRegister;
    }

    public static Map<String, String> revertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = Maps.newHashMap();
        subscribe.forEach((serviceName, serviceQuery) -> {
            if (serviceName.contains(":") || serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String name = serviceName;
                int i = name.indexOf('/');
                if (i >= 0) {
                    params.put("group", name.substring(0, i));
                    name = name.substring(i + 1);
                }
                i = name.lastIndexOf(':');
                if (i >= 0) {
                    params.put("version", name.substring(i + 1));
                    name = name.substring(0, i);
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        });
        return newSubscribe;
    }

    public static Map<String, Map<String, String>> revertNotify(Map<String, Map<String, String>> notify) {
        return buildMap(notify);
    }

    private static Map<String, Map<String, String>> buildMap(Map<String, Map<String, String>> notify) {
        if (CollectionUtils.isEmpty(notify))
            return null;
        Map<String, Map<String, String>> newNotify = Maps.newHashMap();
        notify.forEach((serviceName, serviceUrls) -> {
            if (!serviceName.contains(":") && !serviceName.contains("/") && !CollectionUtils.isEmpty(serviceUrls)) {
                serviceUrls.forEach((url, query) -> {
                    Map<String, String> params = StringUtils.parseQueryString(query);
                    String group = params.get("group");
                    String version = params.get("version");
                    // params.remove("group");
                    // params.remove("version");
                    String name = !Strings.isNullOrEmpty(group) ? group + "/" + serviceName : serviceName;
                    name = !Strings.isNullOrEmpty(version) ? name + ":" + version : name;
                    Map<String, String> newUrls = newNotify.computeIfAbsent(name, k -> Maps.newHashMap());
                    newUrls.put(url, StringUtils.toQueryString(params));
                });
            } else {
                newNotify.put(serviceName, serviceUrls);
            }
        });
        return newNotify;
    }

    //compatible for dubbo-2.0.0
    public static List<String> revertForbid(List<String> forbid, Set<URL> subscribed) {
        if (!CollectionUtils.isEmpty(subscribed)) {
            List<String> newForbid = Lists.newArrayList();
            for (String serviceName : forbid) {
                if (!serviceName.contains(":") && !serviceName.contains("/")) {
                    for (URL url : subscribed) {
                        if (serviceName.equals(url.getServiceInterface())) {
                            newForbid.add(url.getServiceKey());
                            break;
                        }
                    }
                } else {
                    newForbid.add(serviceName);
                }
            }
            return newForbid;
        }
        return forbid;
    }

    public static URL getEmptyUrl(String service, String category) {
        String group = null;
        String version = null;
        int i = service.indexOf('/');
        if (i > 0) {
            group = service.substring(0, i);
            service = service.substring(i + 1);
        }
        i = service.lastIndexOf(':');
        if (i > 0) {
            version = service.substring(i + 1);
            service = service.substring(0, i);
        }
        return URL.valueOf(Constants.EMPTY_PROTOCOL + "://0.0.0.0/" + service + "?"
                + Constants.CATEGORY_KEY + "=" + category
                + (group == null ? "" : "&" + Constants.GROUP_KEY + "=" + group)
                + (version == null ? "" : "&" + Constants.VERSION_KEY + "=" + version));
    }

    public static boolean isMatchCategory(String category, String categories) {
        if (categories == null || categories.length() == 0) {
            return Constants.DEFAULT_CATEGORY.equals(category);
        } else if (categories.contains(Constants.ANY_VALUE)) {
            return true;
        } else if (categories.contains(Constants.REMOVE_VALUE_PREFIX)) {
            return !categories.contains(Constants.REMOVE_VALUE_PREFIX + category);
        } else {
            return categories.contains(category);
        }
    }

    public static boolean isMatch(URL consumerUrl, URL providerUrl) {
        String consumerInterface = consumerUrl.getServiceInterface();
        String providerInterface = providerUrl.getServiceInterface();
        if (!(Constants.ANY_VALUE.equals(consumerInterface) || StringUtils.isEquals(consumerInterface, providerInterface)))
            return false;

        if (!isMatchCategory(providerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY),
                consumerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY))) {
            return false;
        }
        if (!providerUrl.getParameter(Constants.ENABLED_KEY, true)
                && !Constants.ANY_VALUE.equals(consumerUrl.getParameter(Constants.ENABLED_KEY))) {
            return false;
        }

        String consumerGroup = consumerUrl.getParameter(Constants.GROUP_KEY);
        String consumerVersion = consumerUrl.getParameter(Constants.VERSION_KEY);
        String consumerClassifier = consumerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);

        String providerGroup = providerUrl.getParameter(Constants.GROUP_KEY);
        String providerVersion = providerUrl.getParameter(Constants.VERSION_KEY);
        String providerClassifier = providerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);
        return (Constants.ANY_VALUE.equals(consumerGroup) || StringUtils.isEquals(consumerGroup, providerGroup) || StringUtils.isContains(consumerGroup, providerGroup))
                && (Constants.ANY_VALUE.equals(consumerVersion) || StringUtils.isEquals(consumerVersion, providerVersion))
                && (consumerClassifier == null || Constants.ANY_VALUE.equals(consumerClassifier) || StringUtils.isEquals(consumerClassifier, providerClassifier));
    }

    public static boolean isMatchGlobPattern(String pattern, String value, URL param) {
        if (param != null && pattern.startsWith("$")) {
            pattern = param.getRawParameter(pattern.substring(1));
        }
        return isMatchGlobPattern(pattern, value);
    }

    /**
     * 匹配Glob模式。目前的实现只支持<code>*</code>，且只支持一个。不支持<code>?</code>。
     *
     * @return 对于方法参数pattern或是value为<code>null</code>的情况，直接返回<code>false</code>。
     */
    public static boolean isMatchGlobPattern(String pattern, String value) {
        if ("*".equals(pattern))
            return true;
        if (Strings.isNullOrEmpty(pattern) && Strings.isNullOrEmpty(value))
            return true;
        if (Strings.isNullOrEmpty(pattern) || Strings.isNullOrEmpty(value))
            return false;

        int i = pattern.lastIndexOf('*');
        // 没有找到星号
        if (i == -1) {
            return value.equals(pattern);
        }
        // 星号在末尾
        else if (i == pattern.length() - 1) {
            return value.startsWith(pattern.substring(0, i));
        }
        // 星号的开头
        else if (i == 0) {
            return value.endsWith(pattern.substring(i + 1));
        }
        // 星号的字符串的中间
        else {
            String prefix = pattern.substring(0, i);
            String suffix = pattern.substring(i + 1);
            return value.startsWith(prefix) && value.endsWith(suffix);
        }
    }

    public static boolean isServiceKeyMatch(URL pattern, URL value) {
        return pattern.getParameter(Constants.INTERFACE_KEY).equals(value.getParameter(Constants.INTERFACE_KEY))
                && isItemMatch(pattern.getParameter(Constants.GROUP_KEY), value.getParameter(Constants.GROUP_KEY))
                && isItemMatch(pattern.getParameter(Constants.VERSION_KEY), value.getParameter(Constants.VERSION_KEY));
    }

    /**
     * 判断 value 是否匹配 pattern，pattern 支持 * 通配符.
     *
     * @param pattern pattern
     * @param value   value
     * @return true if match otherwise false
     */
    static boolean isItemMatch(String pattern, String value) {
        if (pattern == null) {
            return value == null;
        } else {
            return "*".equals(pattern) || pattern.equals(value);
        }
    }
}