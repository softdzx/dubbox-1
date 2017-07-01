package com.alibaba.dubbo.common.utils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;

abstract public class CollectionUtils {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> List<T> sort(List<T> list) {
        if (isNotEmpty(list)) {
            Collections.sort((List) list);
        }
        return list;
    }

    public static <E> ConcurrentHashSet<E> newConcurrentSet() {
//        Lists.newArrayList()
        return new ConcurrentHashSet<>();
    }

    private static final Comparator<String> SIMPLE_NAME_COMPARATOR = (s1, s2) -> {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return -1;
        }
        if (s2 == null) {
            return 1;
        }
        int i1 = s1.lastIndexOf('.');
        if (i1 >= 0) {
            s1 = s1.substring(i1 + 1);
        }
        int i2 = s2.lastIndexOf('.');
        if (i2 >= 0) {
            s2 = s2.substring(i2 + 1);
        }
        return s1.compareToIgnoreCase(s2);
    };

    public static List<String> sortSimpleName(List<String> list) {
        if (isNotEmpty(list)) {
            list.sort(SIMPLE_NAME_COMPARATOR);
        }
        return list;
    }

    public static Map<String, Map<String, String>> splitAll(Map<String, List<String>> list, String separator) {
        if (list == null) {
            return null;
        }
        Map<String, Map<String, String>> result = Maps.newHashMap();
        list.forEach((key, value) -> result.put(key, split(value, separator)));
        return result;
    }

    public static Map<String, List<String>> joinAll(Map<String, Map<String, String>> map, String separator) {
        if (map == null) {
            return null;
        }
        Map<String, List<String>> result = Maps.newHashMap();
        map.forEach((key, value) -> result.put(key, join(value, separator)));
        return result;
    }

    public static Map<String, String> split(List<String> list, String separator) {
        if (list == null) {
            return null;
        }
        Map<String, String> map = Maps.newHashMap();
        if (list.isEmpty()) {
            return map;
        }
        list.forEach(item -> {
            int index = item.indexOf(separator);
            if (index == -1) {
                map.put(item, "");
            } else {
                map.put(item.substring(0, index), item.substring(index + 1));
            }
        });
        return map;
    }

    public static List<String> join(Map<String, String> map, String separator) {
        if (map == null) {
            return null;
        }
        List<String> list = Lists.newArrayList();
        if (map.isEmpty()) {
            return list;
        }
        map.forEach((key, value) ->
                list.add(Strings.isNullOrEmpty(value) ? key : key + separator + value)
        );
        return list;
    }

    public static String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String ele : list) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(ele);
        }
        return sb.toString();
    }

    public static boolean mapEquals(Map<?, ?> map1, Map<?, ?> map2) {
        if (map1 == null && map2 == null) {
            return true;
        }
        if (map1 == null || map2 == null) {
            return false;
        }
        if (map1.size() != map2.size()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            Object key = entry.getKey();
            Object value1 = entry.getValue();
            Object value2 = map2.get(key);
            if (!objectEquals(value1, value2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean objectEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        return !(obj1 == null || obj2 == null) && obj1.equals(obj2);
    }

    public static Map<String, String> toStringMap(String... pairs) {
        Map<String, String> parameters = Maps.newHashMap();
        if (pairs.length > 0) {
            if (pairs.length % 2 != 0) {
                throw new IllegalArgumentException("pairs must be even.");
            }
            for (int i = 0; i < pairs.length; i = i + 2) {
                parameters.put(pairs[i], pairs[i + 1]);
            }
        }
        return parameters;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Object... pairs) {
        Map<K, V> ret = Maps.newHashMap();
        if (isEmpty(pairs)) return ret;

        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs can not be odd number.");
        }
        int len = pairs.length / 2;
        for (int i = 0; i < len; i++) {
            ret.put((K) pairs[2 * i], (V) pairs[2 * i + 1]);
        }
        return ret;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && collection.size() > 0;
    }

    public static boolean isEmpty(Object[] obj) {
        return null == obj || obj.length == 0;
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return null == map || map.isEmpty();
    }

}