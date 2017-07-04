package com.alibaba.dubbo.governance.web.common.pulltool;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.registry.common.domain.*;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.route.ParseUtils;
import com.alibaba.dubbo.registry.common.route.RouteRule;
import com.alibaba.dubbo.registry.common.route.RouteRule.MatchPair;
import com.alibaba.dubbo.registry.common.util.StringEscapeUtils;
import com.google.common.base.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;

public class Tool {

    private OverrideService overrideService;

    private RouteService routeService;

    public void setOverrideService(OverrideService overrideService) {
        this.overrideService = overrideService;
    }

    public void setRouteService(RouteService routeService) {
        this.routeService = routeService;
    }

    public static String toStackTraceString(Throwable t) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        t.printStackTrace(pw);
        return writer.toString();
    }

    public static boolean isContains(String[] values, String value) {
        return StringUtils.isContains(values, value);
    }

    public static boolean startWith(String value, String prefix) {
        return value.startsWith(prefix);
    }

    public static String getHostPrefix(String address) {
        if (!Strings.isNullOrEmpty(address)) {
            String hostname = getHostName(address);
            if (!address.startsWith(hostname)) {
                return "(" + hostname + ")";
            }
        }
        return "";
    }

    public static String getHostName(String address) {
        return NetUtils.getHostName(address);
    }

    public static String getHostAddress(String address) {
        if (!Strings.isNullOrEmpty(address)) {
            int i = address.indexOf(':');
            String port = address.substring(i + 1);
            String hostname = NetUtils.getHostName(address);
            if (!address.equals(hostname)) {
                return hostname + ":" + port;
            }
        }
        return "";
    }

    public static String getPath(String url) {
        try {
            return URL.valueOf(url).getPath();
        } catch (Throwable t) {
            return url;
        }
    }

    public static String getAddress(String url) {
        try {
            return URL.valueOf(url).getAddress();
        } catch (Throwable t) {
            return url;
        }
    }

    public static String getInterface(String service) {
        if (!Strings.isNullOrEmpty(service)) {
            int i = service.indexOf('/');
            if (i >= 0) {
                service = service.substring(i + 1);
            }
            i = service.lastIndexOf(':');
            if (i >= 0) {
                service = service.substring(0, i);
            }
        }
        return service;
    }

    public static String getGroup(String service) {
        if (!Strings.isNullOrEmpty(service)) {
            int i = service.indexOf('/');
            if (i >= 0) {
                return service.substring(0, i);
            }
        }
        return null;
    }

    public static String getVersion(String service) {
        if (!Strings.isNullOrEmpty(service)) {
            int i = service.lastIndexOf(':');
            if (i >= 0) {
                return service.substring(i + 1);
            }
        }
        return null;
    }

    public static String getIP(String address) {
        if (!Strings.isNullOrEmpty(address)) {
            int i = address.indexOf("://");
            address = i >= 0 ? address.substring(i + 3) : address;

            i = address.indexOf('/');
            address = i >= 0 ? address.substring(0, i) : address;

            i = address.indexOf('@');
            address = i >= 0 ? address.substring(i + 1) : address;

            i = address.indexOf(':');
            address = i >= 0 ? address.substring(0, i) : address;
            if (address.matches("[a-zA-Z]+")) {
                try {
                    address = InetAddress.getByName(address).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        return address;
    }

    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    public static String escape(String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    public static String unescape(String html) {
        return StringEscapeUtils.unescapeHtml(html);
    }

    public static String encodeUrl(String url) {
        return URL.encode(url);
    }

    public static String decodeUrl(String url) {
        return URL.decode(url);
    }

    public static String encodeHtml(String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    public static int countMapValues(Map<?, ?> map) {
        int total = 0;
        if (!CollectionUtils.isEmpty(map)) {
            for (Object value : map.values()) {
                if (null == value)
                    continue;
                int size = 1;
                if (value instanceof Number) {
                    size = ((Number) value).intValue();
                } else if (value.getClass().isArray()) {
                    size = Array.getLength(value);
                } else if (value instanceof Collection) {
                    size = ((Collection<?>) value).size();
                } else if (value instanceof Map) {
                    size = ((Map<?, ?>) value).size();
                }
                total += size;
            }
        }
        return total;
    }

    private static final Comparator<String> SIMPLE_NAME_COMPARATOR = new Comparator<String>() {
        public int compare(String s1, String s2) {
            if (s1 == null && s2 == null) {
                return 0;
            }
            if (s1 == null) {
                return -1;
            }
            if (s2 == null) {
                return 1;
            }
            return getSimpleName(s1).compareToIgnoreCase(getSimpleName(s2));
        }
    };

    public static List<String> sortSimpleName(List<String> list) {
        if (!CollectionUtils.isEmpty(list)) {
            Collections.sort(list, SIMPLE_NAME_COMPARATOR);
        }
        return list;
    }

    public static String getSimpleName(String name) {
        if (!Strings.isNullOrEmpty(name)) {
            final int ip = name.indexOf('/');
            String v = ip != -1 ? name.substring(0, ip + 1) : "";

            int i = name.lastIndexOf(':');
            int j = (i >= 0 ? name.lastIndexOf('.', i) : name.lastIndexOf('.'));
            name = j >= 0 ? name.substring(j + 1) : name;
            name = v + name;
        }
        return name;
    }

    public static String getParameter(String parameters, String key) {
        String value = "";
        if (!Strings.isNullOrEmpty(parameters)) {
            String[] pairs = parameters.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (key.equals(kv[0])) {
                    value = kv[1];
                    break;
                }
            }
        }
        return value;
    }

    public static Map<String, String> toParameterMap(String parameters) {
        return StringUtils.parseQueryString(parameters);
    }


    /**
     * 从provider的paramters参数中获取版本值
     *
     * @param parameters
     * @return
     */
    public static String getVersionFromPara(String parameters) {
        String version = "";
        if (!Strings.isNullOrEmpty(parameters)) {
            String[] params = parameters.split("&");
            for (String o : params) {
                String[] kv = o.split("=");
                if ("version".equals(kv[0])) {
                    version = kv[1];
                    break;
                }
            }
        }
        return version;
    }

    public String formatTimestamp(String timestamp) {
        return Strings.isNullOrEmpty(timestamp) ? "" : formatDate(new Date(Long.valueOf(timestamp)));
    }

    //时间格式化
    public String formatDate(Date date) {
        return null == date ? "" : DateFormatUtil.getDateFormat().format(date);
    }

    public String formatDate(Date date, String template) {
        return null == date || Strings.isNullOrEmpty(template) ? "" : DateFormatUtil.getDateFormat(template).format(date);
    }

    public boolean beforeNow(Date date) {
        Date now = new Date();
        return now.after(date) ? true : false;
    }

    //时间相减
    public long dateMinus(Date date1, Date date2) {
        return (date1.getTime() - date1.getTime()) / 1000;
    }

    public boolean isProviderEnabled(Provider provider) {
        List<Override> oList = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
        return isProviderEnabled(provider, oList);
    }

    public static boolean isProviderEnabled(Provider provider, List<Override> oList) {
        for (Override o : oList) {
            if (o.isMatch(provider)) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                String disbaled = params.get(Constants.DISABLED_KEY);
                if (!Strings.isNullOrEmpty(disbaled)) {
                    return !"true".equals(disbaled);
                }
            }
        }
        return provider.isEnabled();
    }

    public int getProviderWeight(Provider provider) {
        List<Override> oList = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
        return getProviderWeight(provider, oList);
    }

    public static int getProviderWeight(Provider provider, List<Override> oList) {
        for (Override o : oList) {
            if (o.isMatch(provider)) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                String weight = params.get(Constants.WEIGHT_KEY);
                if (!Strings.isNullOrEmpty(weight)) {
                    return Integer.parseInt(weight);
                }
            }
        }
        return provider.getWeight();
    }

    public boolean isInBlackList(Consumer consumer) {
        String service = consumer.getService();
        List<Route> routes = routeService.findForceRouteByService(service);
        if (CollectionUtils.isEmpty(routes)) {
            return false;
        }
        String ip = getIP(consumer.getAddress());
        for (Route route : routes) {
            try {
                if (!route.isEnabled()) {
                    continue;
                }
                String filterRule = route.getFilterRule();
                if (Strings.isNullOrEmpty(filterRule) || "false".equals(filterRule)) {
                    Map<String, MatchPair> rule = RouteRule.parseRule(route.getMatchRule());
                    MatchPair pair = rule.get("consumer.host");
                    if (pair == null) {
                        pair = rule.get("host");
                    }
                    if (pair != null) {
                        Set<String> matches = pair.getMatches();
                        if (!CollectionUtils.isEmpty(matches)) {
                            for (String host : matches) {
                                if (ParseUtils.isMatchGlobPattern(host, ip)) {
                                    return true;
                                }
                            }
                        }
                        Set<String> unmatches = pair.getUnmatches();
                        if (!CollectionUtils.isEmpty(unmatches)) {
                            boolean forbid = true;
                            for (String host : unmatches) {
                                if (ParseUtils.isMatchGlobPattern(host, ip)) {
                                    forbid = false;
                                }
                            }
                            if (forbid) {
                                return true;
                            }
                        }
                    }
                }
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return false;
    }

    public String getConsumerMock(Consumer consumer) {
        return getOverridesMock(consumer.getOverrides());
    }

    public String getOverridesMock(List<Override> overrides) {
        if (!CollectionUtils.isEmpty(overrides)) {
            for (Override override : overrides) {
                Map<String, String> params = StringUtils.parseQueryString(override.getParams());
                String mock = params.get("mock");
                if (!Strings.isNullOrEmpty(mock)) {
                    return mock;
                }
            }
        }
        return "";
    }

    public boolean checkUrl(User user, String uri) {
        return true;
        /*if(!User.ROOT.equals(user.getRole())){
            List<String> disabledSysinfo = new ArrayList<String>();
            List<String> disabledSysmanage = new ArrayList<String>();
            Map<String, Boolean> features = daoCache.getFeatures();
            if (features.size() > 0){
                for(Entry<String,Boolean> feature : features.entrySet()){
                    if(feature.getKey().startsWith("Sysinfo") && !feature.getValue()){
                        disabledSysinfo.add(feature.getKey().replace(".", "/").toLowerCase());
                    }else if(feature.getKey().startsWith("Sysmanage") && !feature.getValue()){
                        disabledSysmanage.add(feature.getKey().replace(".", "/").toLowerCase());
                    }
                }
                if(uri.startsWith("/sysinfo")){
                    for(String disabled : disabledSysinfo){
                        if (uri.contains(disabled)){
                            return false;
                        }
                    }
                }
                if(uri.startsWith("/sysmanage")){
                    for(String disabled : disabledSysmanage){
                        if (uri.contains(disabled)){
                            return false;
                        }
                    }
                }
            }else{
                return true;
            }
        }
        return true;*/
    }
}
