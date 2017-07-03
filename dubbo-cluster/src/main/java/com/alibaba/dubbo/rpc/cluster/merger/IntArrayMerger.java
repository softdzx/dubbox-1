package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class IntArrayMerger implements Merger<int[]>{

    public int[] merge(int[]... items) {
        int totalLen = 0;
        for( int[] item : items ) {
            totalLen += item.length;
        }
        int[] result = new int[totalLen];
        int index = 0;
        for(int[] item : items) {
            for(int i : item) {
                result[index++] = i;
            }
        }
        return result;
    }

}
