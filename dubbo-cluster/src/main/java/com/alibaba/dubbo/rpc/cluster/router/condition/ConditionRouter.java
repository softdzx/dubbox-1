package com.alibaba.dubbo.rpc.cluster.router.condition;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionRouter implements Router, Comparable<Router> {

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);

    private final URL url;

    private final int priority;

    private final boolean force;

    private final Map<String, MatchPair> whenCondition;

    private final Map<String, MatchPair> thenCondition;

    public ConditionRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        this.force = url.getParameter(Constants.FORCE_KEY, false);
        try {
            String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
            if (Strings.isNullOrEmpty(rule)) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            rule = rule.replace("consumer.", "").replace("provider.", "");
            int i = rule.indexOf("=>");
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            Map<String, MatchPair> when = Strings.isNullOrEmpty(whenRule) || "true".equals(whenRule) ? Maps.newHashMap() : parseRule(whenRule);
            Map<String, MatchPair> then = Strings.isNullOrEmpty(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
            // NOTE: When条件是允许为空的，外部业务来保证类似的约束条件
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }
        try {
            if (!matchWhen(url)) {
                return invokers;
            }
            List<Invoker<T>> result = Lists.newArrayList();
            if (thenCondition == null) {
                LogHelper.warn(logger, "The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost()
                        + ", service: " + url.getServiceKey());
                return result;
            }
            for (Invoker<T> invoker : invokers) {
                if (matchThen(invoker.getUrl(), url)) {
                    result.add(invoker);
                }
            }
            if (result.size() > 0) {
                return result;
            } else if (force) {
                LogHelper.warn(logger, "The route result is empty and force execute. consumer: " + NetUtils.getLocalHost()
                        + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(Constants.RULE_KEY));
                return result;
            }
        } catch (Throwable t) {
            LogHelper.error(logger, "Failed to execute condition router rule: " + getUrl() + ", invokers: "
                    + invokers + ", cause: " + t.getMessage(), t);
        }
        return invokers;
    }

    public URL getUrl() {
        return url;
    }

    public int compareTo(Router o) {
        if (o == null || o.getClass() != ConditionRouter.class) {
            return 1;
        }
        ConditionRouter c = (ConditionRouter) o;
        return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
    }

    public boolean matchWhen(URL url) {
        return matchCondition(whenCondition, url, null);
    }

    public boolean matchThen(URL url, URL param) {
        return thenCondition != null && matchCondition(thenCondition, url, param);
    }

    private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param) {
        Map<String, String> sample = url.toMap();
        for (Map.Entry<String, String> entry : sample.entrySet()) {
            String key = entry.getKey();
            MatchPair pair = condition.get(key);
            if (pair != null && !pair.isMatch(entry.getValue(), param)) {
                return false;
            }
        }
        return true;
    }

    private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");

    private static Map<String, MatchPair> parseRule(String rule)
            throws ParseException {
        Map<String, MatchPair> condition = Maps.newHashMap();
        if (Strings.isNullOrEmpty(rule)) {
            return condition;
        }
        // 匹配或不匹配Key-Value对
        MatchPair pair = null;
        // 多个Value值
        Set<String> values = null;
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        while (matcher.find()) { // 逐个匹配
            String separator = matcher.group(1);
            String content = matcher.group(2);
            // 表达式开始
            if (Strings.isNullOrEmpty(separator)) {
                pair = new MatchPair();
                condition.put(content, pair);
            }
            // KV开始
            else if ("&".equals(separator)) {
                if (condition.get(content) == null) {
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    condition.put(content, pair);
                }
            }
            // KV的Value部分开始
            else {
                if ("=".equals(separator)) {
                    if (pair == null)
                        throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator
                                + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
                    values = pair.matches;
                    values.add(content);
                }
                // KV的Value部分开始
                else if ("!=".equals(separator)) {
                    if (pair == null)
                        throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator
                                + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
                    values = pair.mismatches;
                    values.add(content);
                }
                // KV的Value部分的多个条目
                else if (",".equals(separator)) { // 如果为逗号表示
                    if (CollectionUtils.isEmpty(values))
                        throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator
                                + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
                    values.add(content);
                } else {
                    throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index "
                            + matcher.start() + " before \"" + content + "\".", matcher.start());
                }
            }
        }
        return condition;
    }

    private static final class MatchPair {
        final Set<String> matches = Sets.newHashSet();
        final Set<String> mismatches = Sets.newHashSet();

        public boolean isMatch(String value, URL param) {
            for (String match : matches) {
                if (!UrlUtils.isMatchGlobPattern(match, value, param)) {
                    return false;
                }
            }
            for (String mismatch : mismatches) {
                if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                    return false;
                }
            }
            return true;
        }
    }
}