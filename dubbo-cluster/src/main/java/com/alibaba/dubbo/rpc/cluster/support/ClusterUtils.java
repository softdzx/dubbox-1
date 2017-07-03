package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Map;

public class ClusterUtils {

    public static URL mergeUrl(URL remoteUrl, Map<String, String> localMap) {
        Map<String, String> map = Maps.newHashMap();
        Map<String, String> remoteMap = remoteUrl.getParameters();


        if (!CollectionUtils.isEmpty(remoteMap)) {
            map.putAll(remoteMap);

            //线程池配置不使用提供者的
            map.remove(Constants.THREAD_NAME_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY);

            map.remove(Constants.THREADPOOL_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREADPOOL_KEY);

            map.remove(Constants.CORE_THREADS_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.CORE_THREADS_KEY);

            map.remove(Constants.THREADS_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREADS_KEY);

            map.remove(Constants.QUEUES_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.QUEUES_KEY);

            map.remove(Constants.ALIVE_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.ALIVE_KEY);
        }

        if (!CollectionUtils.isEmpty(localMap)) {
            map.putAll(localMap);
        }
        if (!CollectionUtils.isEmpty(remoteMap)) {
            // 版本号使用提供者的
            String dubbo = remoteMap.get(Constants.DUBBO_VERSION_KEY);
            if (!Strings.isNullOrEmpty(dubbo)) {
                map.put(Constants.DUBBO_VERSION_KEY, dubbo);
            }
            String version = remoteMap.get(Constants.VERSION_KEY);
            if (version != null && version.length() > 0) {
                map.put(Constants.VERSION_KEY, version);
            }
            String group = remoteMap.get(Constants.GROUP_KEY);
            if (!Strings.isNullOrEmpty(group)) {
                map.put(Constants.GROUP_KEY, group);
            }
            String methods = remoteMap.get(Constants.METHODS_KEY);
            if (!Strings.isNullOrEmpty(methods)) {
                map.put(Constants.METHODS_KEY, methods);
            }
            // 合并filter和listener
            String remoteFilter = remoteMap.get(Constants.REFERENCE_FILTER_KEY);
            String localFilter = localMap.get(Constants.REFERENCE_FILTER_KEY);
            if (!Strings.isNullOrEmpty(remoteFilter) && !Strings.isNullOrEmpty(localFilter)) {
                localMap.put(Constants.REFERENCE_FILTER_KEY, remoteFilter + "," + localFilter);
            }
            String remoteListener = remoteMap.get(Constants.INVOKER_LISTENER_KEY);
            String localListener = localMap.get(Constants.INVOKER_LISTENER_KEY);
            if (!Strings.isNullOrEmpty(remoteListener) && !Strings.isNullOrEmpty(localListener)) {
                localMap.put(Constants.INVOKER_LISTENER_KEY, remoteListener + "," + localListener);
            }
        }

        return remoteUrl.clearParameters().addParameters(map);
    }

    private ClusterUtils() {
    }

}