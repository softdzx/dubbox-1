package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class FloatArrayMerger implements Merger<float[]> {

    public float[] merge(float[]... items) {
        int total = 0;
        for (float[] array : items) {
            total += array.length;
        }
        float[] result = new float[total];
        int index = 0;
        for (float[] array : items) {
            for (float item : array) {
                result[index++] = item;
            }
        }
        return result;
    }
}
