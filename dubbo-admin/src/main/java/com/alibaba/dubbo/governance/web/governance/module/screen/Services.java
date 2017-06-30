package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.route.OverrideUtils;
import com.alibaba.dubbo.registry.common.util.Tool;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Providers. URI: /services/$service/providers /addresses/$address/services /application/$application/services
 *
 * @author ding.lid
 */
public class Services extends Restful {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private OverrideService overrideService;

    public void index(Map<String, Object> context) {
        String application = (String) context.get("application");
        String address = (String) context.get("address");

        if (context.get("service") == null
                && context.get("application") == null
                && context.get("address") == null) {
            context.put("service", "*");
        }

        List<String> providerServices = null;
        List<String> consumerServices = null;
        List<Override> overrides = null;
        if (!Strings.isNullOrEmpty(application)) {
            providerServices = providerService.findServicesByApplication(application);
            consumerServices = consumerService.findServicesByApplication(application);
            overrides = overrideService.findByApplication(application);
        } else if (!Strings.isNullOrEmpty(address)) {
            providerServices = providerService.findServicesByAddress(address);
            consumerServices = consumerService.findServicesByAddress(address);
            overrides = overrideService.findByAddress(Tool.getIP(address));
        } else {
            providerServices = providerService.findServices();
            consumerServices = consumerService.findServices();
            overrides = overrideService.findAll();
        }

        Set<String> services = Sets.newTreeSet();
        if (providerServices != null) {
            services.addAll(providerServices);
        }
        if (consumerServices != null) {
            services.addAll(consumerServices);
        }

        Map<String, List<Override>> service2Overrides = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(overrides) && !CollectionUtils.isEmpty(services)) {
            for (String s : services) {
                for (Override override : overrides) {
                    List<Override> serOverrides = Lists.newArrayList();
                    if (override.isMatch(s, address, application)) {
                        serOverrides.add(override);
                    }
                    Collections.sort(serOverrides, OverrideUtils.OVERRIDE_COMPARATOR);
                    service2Overrides.put(s, serOverrides);
                }
            }
        }

        context.put("providerServices", providerServices);
        context.put("consumerServices", consumerServices);
        context.put("services", services);
        context.put("overrides", service2Overrides);

        String keyword = (String) context.get("keyword");
        if (!Strings.isNullOrEmpty(keyword) && !"*".equals(keyword)) {
            keyword = keyword.toLowerCase();
            Set<String> newList = Sets.newHashSet();
            Set<String> newProviders = Sets.newHashSet();
            Set<String> newConsumers = Sets.newHashSet();

            for (String o : services) {
                if (o.toLowerCase().toLowerCase().contains(keyword)) {
                    newList.add(o);
                }
            }
            for (String o : providerServices) {
                if (o.toLowerCase().contains(keyword)) {
                    newProviders.add(o);
                }
            }
            for (String o : consumerServices) {
                if (o.toLowerCase().contains(keyword)) {
                    newConsumers.add(o);
                }
            }
            context.put("services", newList);
            context.put("providerServices", newProviders);
            context.put("consumerServices", newConsumers);
        }
    }

    public boolean shield(Map<String, Object> context) throws Exception {
        return mock(context, "force:return null");
    }

    public boolean tolerant(Map<String, Object> context) throws Exception {
        return mock(context, "fail:return null");
    }

    public boolean recover(Map<String, Object> context) throws Exception {
        return mock(context, "");
    }

    private boolean mock(Map<String, Object> context, String mock) throws Exception {
        String services = (String) context.get("service");
        String application = (String) context.get("application");
        if (Strings.isNullOrEmpty(services) || Strings.isNullOrEmpty(application)) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        for (String service : SPACE_SPLIT_PATTERN.split(services)) {
            if (!super.currentUser.hasServicePrivilege(service)) {
                context.put("message", getMessage("HaveNoServicePrivilege", service));
                return false;
            }
        }
        for (String service : SPACE_SPLIT_PATTERN.split(services)) {
            List<Override> overrides = overrideService.findByServiceAndApplication(service, application);
            if (overrides != null && overrides.size() > 0) {
                for (Override override : overrides) {
                    Map<String, String> map = StringUtils.parseQueryString(override.getParams());
                    if (mock == null || mock.length() == 0) {
                        map.remove("mock");
                    } else {
                        map.put("mock", URL.encode(mock));
                    }
                    if (map.size() > 0) {
                        override.setParams(StringUtils.toQueryString(map));
                        override.setEnabled(true);
                        override.setOperator(operator);
                        override.setOperatorAddress(operatorAddress);
                        overrideService.updateOverride(override);
                    } else {
                        overrideService.deleteOverride(override.getId());
                    }
                }
            } else if (mock != null && mock.length() > 0) {
                Override override = new Override();
                override.setService(service);
                override.setApplication(application);
                override.setParams("mock=" + URL.encode(mock));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.saveOverride(override);
            }
        }
        return true;
    }

}
