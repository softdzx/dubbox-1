package com.alibaba.dubbo.rpc.cluster.merger;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.dubbo.rpc.cluster.Merger;
import com.google.common.collect.Sets;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class SetMerger implements Merger<Set<?>> {

    public Set<Object> merge(Set<?>... items) {

        Set<Object> result = Sets.newHashSet();
        for (Set<?> item : items) {
            if (item != null) {
                result.addAll(item);
            }
        }
        return result;
    }
}
