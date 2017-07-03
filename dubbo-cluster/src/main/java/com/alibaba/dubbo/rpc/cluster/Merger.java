package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@SPI
public interface Merger<T> {

    T merge(T... items);

}
