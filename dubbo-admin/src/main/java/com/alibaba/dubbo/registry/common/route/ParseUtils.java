package com.alibaba.dubbo.registry.common.route;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串解析相关的工具方法，涉及interpolation、Glob模式、Query字串、Service URL处理。
 *
 * @author william.liangf
 * @author ding.lid
 */
public class ParseUtils {

    public static String METHOD_SPLIT = ",";

    private ParseUtils() {
    }

    private static Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\$\\s*\\{?\\s*([\\._0-9a-zA-Z]+)\\s*\\}?");

    /**
     * 执行interpolation(变量插入)。
     *
     * @param expression 含有变量的表达式字符串。表达式中的变量名也可以用<code>{}</code>括起来。
     * @param params     变量集。变量名可以包含<code>.</code>、<code>_</code>字符。
     * @return 完成interpolation后的字符串。 如：<code><pre>xxx${name}zzz -> xxxjerryzzz</pre></code>（其中变量name="jerry"）
     * @throws IllegalStateException 表达式字符串中使用到的变量 在变量集中没有
     */
    // FIMXE 抛出IllegalStateException异常，是否合适？！
    public static String interpolate(String expression, Map<String, String> params) {
        if (Strings.isNullOrEmpty(expression)) {
            throw new IllegalArgumentException("glob pattern is empty!");
        }
        if (expression.indexOf('$') < 0) {
            return expression;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) { // 逐个匹配
            String key = matcher.group(1);
            String value = params == null ? null : params.get(key);
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static List<String> interpolate(List<String> expressions, Map<String, String> params) {
        List<String> ret = Lists.newArrayList();

        if (CollectionUtils.isEmpty(expressions)) {
            return ret;
        }

        for (String expr : expressions) {
            ret.add(interpolate(expr, params));
        }

        return ret;
    }

    /**
     * 是否匹配Glob模式。Glob模式是要插值的表达式。Glob模式有多个，只要匹配一个模式，就认为匹配成功。
     *
     * @param patternsNeedInterpolate 多个要进行插值的Glob模式
     * @param interpolateParams       用于插值的变量集
     * @param value                   进行Glob模式的值
     */
    public static boolean isMatchGlobPatternsNeedInterpolate(
            Collection<String> patternsNeedInterpolate,
            Map<String, String> interpolateParams, String value) {
        if (patternsNeedInterpolate != null && !patternsNeedInterpolate.isEmpty()) {
            for (String patternNeedItp : patternsNeedInterpolate) {
                if (Strings.isNullOrEmpty(patternNeedItp)) {
                    continue;
                }
                // FIXME ERROR!! 原来的实现，这里只和第一个不为空的pattern比较，返回对应的结果！ 和梁飞确认
                String pattern = interpolate(patternNeedItp, interpolateParams);
                if (UrlUtils.isMatchGlobPattern(pattern, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回集合中与Glob模式匹配的条目。
     */
    public static Set<String> filterByGlobPattern(String pattern, Collection<String> values) {
        Set<String> ret = Sets.newHashSet();
        if (pattern == null || values == null) {
            return ret;
        }

        for (String v : values) {
            if (UrlUtils.isMatchGlobPattern(pattern, v)) {
                ret.add(v);
            }
        }
        return ret;
    }

    /**
     * 找到了配合Glob模式的字符串。模式有多个，只要匹配一个模式，就返回这个字符串。
     */
    public static Set<String> filterByGlobPattern(Collection<String> patterns, Collection<String> values) {
        Set<String> ret = Sets.newHashSet();
        if (CollectionUtils.isEmpty(patterns) || CollectionUtils.isEmpty(values)) {
            return ret;
        }

        for (String p : patterns) {
            for (String v : values) {
                if (UrlUtils.isMatchGlobPattern(p, v)) {
                    ret.add(v);
                }
            }
        }
        return ret;
    }

    /**
     * 两个Glob模式是否有交集。
     */
    public static boolean hasIntersection(String glob1, String glob2) {
        if (null == glob1 || null == glob2) {
            return false;
        }

        if (glob1.contains("*") && glob2.contains("*")) {
            int index1 = glob1.indexOf("*");
            int index2 = glob2.indexOf("*");

            String s11 = glob1.substring(0, index1);
            String s12 = glob1.substring(index1 + 1, glob1.length());

            String s21 = glob2.substring(0, index2);
            String s22 = glob2.substring(index2 + 1, glob2.length());

            if (!s11.startsWith(s21) && !s21.startsWith(s11)) return false;
            if (!s12.endsWith(s22) && !s22.endsWith(s12)) return false;
            return true;
        } else if (glob1.contains("*")) {
            return UrlUtils.isMatchGlobPattern(glob1, glob2);
        } else if (glob2.contains("*")) {
            return UrlUtils.isMatchGlobPattern(glob2, glob1);
        } else {
            return glob1.equals(glob2);
        }
    }

    private static Pattern QUERY_PATTERN = Pattern
            .compile("([&=]?)\\s*([^&=\\s]+)");

    /**
     * 把Query String解析成Map。对于有只有Key的串<code>key3=</code>，忽略。
     *
     * @param keyPrefix 在输出的Map的Key加上统一前缀。
     * @param query     Query String，形如：<code>key1=value1&key2=value2</code>
     * @return Query String为<code>key1=value1&key2=value2</code>，前缀为<code>pre.</code>时，
     * 则返回<code>Map{pre.key1=value1, pre.key=value2}</code>。
     */
    // FIXME 抛出的是IllegalStateException异常，是否合理？！
    public static Map<String, String> parseQuery(String keyPrefix, String query) {
        if (query == null)
            return Maps.newHashMap();
        if (keyPrefix == null)
            keyPrefix = "";

        Matcher matcher = QUERY_PATTERN.matcher(query);
        Map<String, String> routeQuery = Maps.newHashMap();
        String key = null;
        while (matcher.find()) { // 逐个匹配
            String separator = matcher.group(1);
            String content = matcher.group(2);
            if (Strings.isNullOrEmpty(separator) || "&".equals(separator)) {
                if (key != null)
                    throw new IllegalStateException("Illegal query string \"" + query + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \"" + content + "\".");
                key = content;
            } else if ("=".equals(separator)) {
                if (key == null)
                    throw new IllegalStateException("Illegal query string \"" + query + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \"" + content + "\".");
                routeQuery.put(keyPrefix + key, content);
                key = null;
            } else {
                if (key == null)
                    throw new IllegalStateException("Illegal query string \"" + query + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \"" + content + "\".");
            }
        }
        return routeQuery;
    }

    public static Map<String, String> parseQuery(String query) {
        return parseQuery("", query);
    }

    private static final ConcurrentMap<String, Pattern> REPLACE_PARAMETER_PATTERNS = Maps.newConcurrentMap();

    /**
     * 替换url中参数的值。
     */
    public static String replaceParameter(String query, String key, String value) {
        if (Strings.isNullOrEmpty(query)) {
            return key + "=" + value;
        }
        if (!query.contains(key + "=")) {
            return query + "&" + key + "=" + value;
        }
        Pattern pattern = REPLACE_PARAMETER_PATTERNS.get(key);
        if (pattern == null) {
            pattern = Pattern.compile(key.replaceAll("([^(_0-9A-Za-z)])", "\\\\$0") + "=[^&]+");
        }
        Matcher matcher = pattern.matcher(query);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, (key + "=" + value).replace("$", "\\$"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String appendParamToUri(String uri, String name, String value) {
        if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(value)) return uri;
        if (uri.indexOf('?') != -1) {
            uri += "&" + name + "=" + value;
        } else {
            uri += "?" + name + "=" + value;
        }
        return uri;
    }

    public static String appendParamsToUri(String uri, Map<String, String> params) {
        StringBuilder buf = new StringBuilder(uri);
        boolean first = (uri.indexOf('?') < 0);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(value))
                continue;
            if (first) {
                buf.append("?");
                first = false;
            } else {
                buf.append("&");
            }
            buf.append(key);
            buf.append("=");
            buf.append(value);
        }
        return buf.toString();
    }

    public static boolean matchEndStarPattern(String value, String pattern) {
        if (!pattern.endsWith("*")) throw new IllegalArgumentException("not end star pattern!");
        String perfix = pattern.substring(0, pattern.length() - 1);
        return value.startsWith(perfix);
    }
}
