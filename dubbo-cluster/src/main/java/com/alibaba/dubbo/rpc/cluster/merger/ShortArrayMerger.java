package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ShortArrayMerger implements Merger<short[]> {

    public short[] merge(short[]... items) {
        int total = 0;
        for (short[] array : items) {
            total += array.length;
        }
        short[] result = new short[total];
        int index = 0;
        for (short[] array : items) {
            for (short item : array) {
                result[index++] = item;
            }
        }
        return result;
    }
}
