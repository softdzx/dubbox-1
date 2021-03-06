package com.alibaba.dubbo.governance.sync;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.web.common.pulltool.Tool;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class RegistryServerSync implements InitializingBean, DisposableBean, NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServerSync.class);

    private static final URL SUBSCRIBE = new URL(Constants.ADMIN_PROTOCOL, NetUtils.getLocalHost(), 0, "",
            Constants.INTERFACE_KEY, Constants.ANY_VALUE,
            Constants.GROUP_KEY, Constants.ANY_VALUE,
            Constants.VERSION_KEY, Constants.ANY_VALUE,
            Constants.CLASSIFIER_KEY, Constants.ANY_VALUE,
            Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY + ","
            + Constants.CONSUMERS_CATEGORY + ","
            + Constants.ROUTERS_CATEGORY + ","
            + Constants.CONFIGURATORS_CATEGORY,
            Constants.ENABLED_KEY, Constants.ANY_VALUE,
            Constants.CHECK_KEY, String.valueOf(false));

    private static final AtomicLong ID = new AtomicLong();

    @Autowired
    private RegistryService registryService;

    // ConcurrentMap<category, ConcurrentMap<servicename, Map<Long, URL>>>
    private final ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> registryCache = Maps.newConcurrentMap();

    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getRegistryCache() {
        return registryCache;
    }

    public void afterPropertiesSet() throws Exception {
        logger.info("Init Dubbo Admin Sync Cache...");
        registryService.subscribe(SUBSCRIBE, this);
    }

    public void destroy() throws Exception {
        registryService.unsubscribe(SUBSCRIBE, this);
    }

    // 收到的通知对于 ，同一种类型数据（override、subcribe、route、其它是Provider），同一个服务的数据是全量的
    public void notify(List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return;
        }
        final Map<String, Map<String, Map<Long, URL>>> categories = Maps.newHashMap();
        for (URL url : urls) {
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
            if (Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) { // 注意：empty协议的group和version为*
                ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
                if (!CollectionUtils.isEmpty(services)) {
                    String group = url.getParameter(Constants.GROUP_KEY);
                    String version = url.getParameter(Constants.VERSION_KEY);
                    // 注意：empty协议的group和version为*
                    if (!Constants.ANY_VALUE.equals(group) && !Constants.ANY_VALUE.equals(version)) {
                        services.remove(url.getServiceKey());
                    } else {
                        for (Map.Entry<String, Map<Long, URL>> serviceEntry : services.entrySet()) {
                            String service = serviceEntry.getKey();
                            if (Tool.getInterface(service).equals(url.getServiceInterface())
                                    && (Constants.ANY_VALUE.equals(group) || StringUtils.isEquals(group, Tool.getGroup(service)))
                                    && (Constants.ANY_VALUE.equals(version) || StringUtils.isEquals(version, Tool.getVersion(service)))) {
                                services.remove(service);
                            }
                        }
                    }
                }
            } else {
                Map<String, Map<Long, URL>> services = categories.putIfAbsent(category, Maps.newHashMap());
                String service = url.getServiceKey();
                Map<Long, URL> ids = services.putIfAbsent(service, Maps.newHashMap());
                ids.put(ID.incrementAndGet(), url);
            }
        }
        categories.forEach((category, categoryEntry) -> {
            ConcurrentMap<String, Map<Long, URL>> services = registryCache.putIfAbsent(category, Maps.newConcurrentMap());
            services.putAll(categoryEntry);
        });
//        for (Map.Entry<String, Map<String, Map<Long, URL>>> categoryEntry : categories.entrySet()) {
//            String category = categoryEntry.getKey();
//            ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
//            if (services == null) {
//                services = new ConcurrentHashMap<String, Map<Long, URL>>();
//                registryCache.put(category, services);
//            }
//            services.putAll(categoryEntry.getValue());
//        }
    }
}
    
