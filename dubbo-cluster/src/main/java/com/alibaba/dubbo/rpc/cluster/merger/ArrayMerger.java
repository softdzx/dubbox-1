package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ArrayMerger implements Merger<Object[]> {

    public static final ArrayMerger INSTANCE = new ArrayMerger();

    public Object[] merge(Object[]... others) {
        if (others.length == 0) {
            return null;
        }
        int totalLen = 0;
        for (int i = 0; i < others.length; i++) {
            Object item = others[i];
            if (item != null && item.getClass().isArray()) {
                totalLen += Array.getLength(item);
            } else {
                throw new IllegalArgumentException(i + 1 + "the argument is not an array");
            }
        }

        if (totalLen == 0) {
            return null;
        }

        Class<?> type = others[0].getClass().getComponentType();

        Object result = Array.newInstance(type, totalLen);
        int index = 0;
        for (Object array : others) {
            for (int i = 0; i < Array.getLength(array); i++) {
                Array.set(result, index++, Array.get(array, i));
            }
        }
        return (Object[]) result;
    }

}
