package com.alibaba.dubbo.rpc.cluster.merger;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.rpc.cluster.Merger;
import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ListMerger implements Merger<List<?>> {

    public List<Object> merge(List<?>... items) {
        List<Object> result = Lists.newArrayList();
        for (List<?> item : items) {
            if (item != null) {
                result.addAll(item);
            }
        }
        return result;
    }

}
