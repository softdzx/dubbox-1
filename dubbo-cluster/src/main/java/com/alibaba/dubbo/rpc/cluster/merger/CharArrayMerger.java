package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class CharArrayMerger implements Merger<char[]> {

    public char[] merge(char[]... items) {
        int total = 0;
        for (char[] array : items) {
            total += array.length;
        }
        char[] result = new char[total];
        int index = 0;
        for (char[] array : items) {
            for (char item : array) {
                result[index++] = item;
            }
        }
        return result;
    }
}
