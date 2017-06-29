package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Providers.
 * URI: /services/$service/providers
 *
 * @author william.liangf
 */
public class Addresses extends Restful {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    public void index(Map<String, Object> context) {
        String application = (String) context.get("application");
        String service = (String) context.get("service");
        List<String> providerAddresses = null;
        List<String> consumerAddresses = null;

        if (!Strings.isNullOrEmpty(application)) {
            providerAddresses = providerService.findAddressesByApplication(application);
            consumerAddresses = consumerService.findAddressesByApplication(application);
        } else if (!Strings.isNullOrEmpty(service)) {
            providerAddresses = providerService.findAddressesByService(service);
            consumerAddresses = consumerService.findAddressesByService(service);
        } else {
            providerAddresses = providerService.findAddresses();
            consumerAddresses = consumerService.findAddresses();
        }

        Set<String> addresses = Sets.newTreeSet();
        if (providerAddresses != null) {
            addresses.addAll(providerAddresses);
        }
        if (consumerAddresses != null) {
            addresses.addAll(consumerAddresses);
        }
        context.put("providerAddresses", providerAddresses);
        context.put("consumerAddresses", consumerAddresses);
        context.put("addresses", addresses);

        if (context.get("service") == null
                && context.get("application") == null
                && context.get("address") == null) {
            context.put("address", "*");
        }

        String keyword = (String) context.get("keyword");
        if (!Strings.isNullOrEmpty(keyword)) {
            if ("*".equals(keyword)) return;

            keyword = keyword.toLowerCase();
            Set<String> newList = Sets.newHashSet();
            Set<String> newProviders = Sets.newHashSet();
            Set<String> newConsumers = Sets.newHashSet();

            for (String o : addresses) {
                if (o.toLowerCase().contains(keyword)) {
                    newList.add(o);
                }
            }
            for (String o : providerAddresses) {
                if (o.toLowerCase().contains(keyword)) {
                    newProviders.add(o);
                }
            }
            for (String o : consumerAddresses) {
                if (o.toLowerCase().contains(keyword)) {
                    newConsumers.add(o);
                }
            }
            context.put("addresses", newList);
            context.put("providerAddresses", newProviders);
            context.put("consumerAddresses", newConsumers);
        }
    }

    public void search(Map<String, Object> context) {
        index(context);

        Set<String> newList = Sets.newHashSet();
        @SuppressWarnings("unchecked")
        Set<String> list = (Set<String>) context.get("addresses");
        String keyword = (String) context.get("keyword");
        if (!Strings.isNullOrEmpty(keyword)) {
            keyword = keyword.toLowerCase();
            for (String o : list) {
                if (o.toLowerCase().contains(keyword)) {
                    newList.add(o);
                }
            }
        }
        context.put("addresses", newList);
    }
}
