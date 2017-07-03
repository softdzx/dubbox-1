package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.sync.util.Pair;
import com.alibaba.dubbo.governance.sync.util.SyncUtils;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.route.ParseUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

public class ProviderServiceImpl extends AbstractService implements ProviderService {

    @Autowired
    OverrideService overrideService;

    public void create(Provider provider) {
        URL url = provider.toUrl();
        registryService.register(url);
    }

    public void enableProvider(Long id) {
        if (id == null) {
            throw new IllegalStateException("no provider id");
        }

        Provider oldProvider = findProvider(id);

        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        if (oldProvider.isDynamic()) {
            //保证disable的override唯一
            if (!oldProvider.isEnabled()) {
                Override override = new Override();
                override.setAddress(oldProvider.getAddress());
                override.setService(oldProvider.getService());
                override.setEnabled(true);
                override.setParams(Constants.DISABLED_KEY + "=false");
                overrideService.saveOverride(override);
                return;
            }
            List<Override> oList = overrideService.findByServiceAndAddress(oldProvider.getService(), oldProvider.getAddress());

            for (Override o : oList) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                if (params.containsKey(Constants.DISABLED_KEY)) {
                    if (params.get(Constants.DISABLED_KEY).equals("true")) {
                        overrideService.deleteOverride(o.getId());
                    }
                }
            }
        } else {
            oldProvider.setEnabled(true);
            updateProvider(oldProvider);
        }
    }

    public void disableProvider(Long id) {
        if (id == null) {
            throw new IllegalStateException("no provider id");
        }

        Provider oldProvider = findProvider(id);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }

        if (oldProvider.isDynamic()) {
            //保证disable的override唯一
            if (oldProvider.isEnabled()) {
                Override override = new Override();
                override.setAddress(oldProvider.getAddress());
                override.setService(oldProvider.getService());
                override.setEnabled(true);
                override.setParams(Constants.DISABLED_KEY + "=true");
                overrideService.saveOverride(override);
                return;
            }
            List<Override> oList = overrideService.findByServiceAndAddress(oldProvider.getService(), oldProvider.getAddress());

            for (Override o : oList) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                if (params.containsKey(Constants.DISABLED_KEY)) {
                    if (params.get(Constants.DISABLED_KEY).equals("false")) {
                        overrideService.deleteOverride(o.getId());
                    }
                }
            }
        } else {
            oldProvider.setEnabled(false);
            updateProvider(oldProvider);
        }

    }

    public void doublingProvider(Long id) {
        setWeight(id, 2F);
    }

    public void halvingProvider(Long id) {
        setWeight(id, 0.5F);
    }

    public void setWeight(Long id, float factor) {
        if (id == null) {
            throw new IllegalStateException("no provider id");
        }
        Provider oldProvider = findProvider(id);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        Map<String, String> map = StringUtils.parseQueryString(oldProvider.getParameters());
        String weight = map.get(Constants.WEIGHT_KEY);
        if (oldProvider.isDynamic()) {
            //保证disable的override唯一
            List<Override> overrides = overrideService.findByServiceAndAddress(oldProvider.getService(), oldProvider.getAddress());
            if (CollectionUtils.isEmpty(overrides)) {
                int value = getWeight(weight, factor);
                if (value != Constants.DEFAULT_WEIGHT) {
                    Override override = new Override();
                    override.setAddress(oldProvider.getAddress());
                    override.setService(oldProvider.getService());
                    override.setEnabled(true);
                    override.setParams(Constants.WEIGHT_KEY + "=" + String.valueOf(value));
                    overrideService.saveOverride(override);
                }
            } else {
                for (Override override : overrides) {
                    Map<String, String> params = StringUtils.parseQueryString(override.getParams());
                    String overrideWeight = params.get(Constants.WEIGHT_KEY);
                    if (Strings.isNullOrEmpty(overrideWeight)) {
                        overrideWeight = weight;
                    }
                    int value = getWeight(overrideWeight, factor);
                    if (value == getWeight(weight, 1)) {
                        params.remove(Constants.WEIGHT_KEY);
                    } else {
                        params.put(Constants.WEIGHT_KEY, String.valueOf(value));
                    }
                    if (params.size() > 0) {
                        override.setParams(StringUtils.toQueryString(params));
                        overrideService.updateOverride(override);
                    } else {
                        overrideService.deleteOverride(override.getId());
                    }
                }
            }
        } else {
            int value = getWeight(weight, factor);
            if (value == Constants.DEFAULT_WEIGHT) {
                map.remove(Constants.WEIGHT_KEY);
            } else {
                map.put(Constants.WEIGHT_KEY, String.valueOf(value));
            }
            oldProvider.setParameters(StringUtils.toQueryString(map));
            updateProvider(oldProvider);
        }
    }

    private int getWeight(String value, float factor) {
        int weight = 100;
        if (!Strings.isNullOrEmpty(value)) {
            weight = Integer.parseInt(value);
        }
        weight = (int) (weight * factor);
        if (weight < 1) weight = 1;
        if (weight == 2) weight = 3;
        if (weight == 24) weight = 25;
        return weight;
    }

    public void deleteStaticProvider(Long id) {
        URL oldProvider = findProviderUrl(id);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        registryService.unregister(oldProvider);
    }

    public void updateProvider(Provider provider) {
        Long id = provider.getId();
        if (id == null) {
            throw new IllegalStateException("no provider id");
        }

        URL oldProvider = findProviderUrl(id);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        URL newProvider = provider.toUrl();

        registryService.unregister(oldProvider);
        registryService.register(newProvider);
    }

    public Provider findProvider(Long id) {
        return SyncUtils.url2Provider(findProviderUrlPair(id));
    }

    public Pair<Long, URL> findProviderUrlPair(Long id) {
        return SyncUtils.filterFromCategory(getRegistryCache(), Constants.PROVIDERS_CATEGORY, id);
    }

    public List<String> findServices() {
        List<String> ret = Lists.newArrayList();
        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls != null) ret.addAll(providerUrls.keySet());
        return ret;
    }

    public List<String> findAddresses() {
        List<String> ret = Lists.newArrayList();

        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (null == providerUrls) return ret;

        providerUrls.forEach((key, urlMaps) -> urlMaps.forEach((k, url) -> {
            String app = url.getAddress();
            if (!Strings.isNullOrEmpty(app))
                ret.add(app);
        }));
        return ret;
    }

    public List<String> findAddressesByApplication(String application) {
        List<String> ret = Lists.newArrayList();
        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        providerUrls.forEach((key, value) -> value.forEach((k, url) -> {
            if (application.equals(url.getParameter(Constants.APPLICATION_KEY))) {
                String addr = url.getAddress();
                if (!Strings.isNullOrEmpty(addr))
                    ret.add(addr);
            }
        }));
        return ret;
    }

    public List<String> findAddressesByService(String service) {
        List<String> ret = Lists.newArrayList();
        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (null == providerUrls) return ret;

        providerUrls.get(service).forEach((key, url) -> {
            String app = url.getAddress();
            if (!Strings.isNullOrEmpty(app))
                ret.add(app);
        });
        return ret;
    }

    public List<String> findApplicationsByServiceName(String service) {
        List<String> ret = Lists.newArrayList();
        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (null == providerUrls) return ret;

        Map<Long, URL> value = providerUrls.get(service);
        if (value == null) {
            return ret;
        }
        value.forEach((key, url) -> {
            String app = url.getParameter(Constants.APPLICATION_KEY);
            if (!Strings.isNullOrEmpty(app))
                ret.add(app);
        });
        return ret;
    }

    public List<Provider> findByService(String serviceName) {
        return SyncUtils.url2ProviderList(findProviderUrlByService(serviceName));
    }

    private Map<Long, URL> findProviderUrlByService(String service) {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Provider> findAll() {
        return SyncUtils.url2ProviderList(findAllProviderUrl());
    }

    private Map<Long, URL> findAllProviderUrl() {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Provider> findByAddress(String providerAddress) {
        return SyncUtils.url2ProviderList(findProviderUrlByAddress(providerAddress));
    }

    public Map<Long, URL> findProviderUrlByAddress(String address) {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<String> findServicesByAddress(String address) {
        List<String> ret = Lists.newArrayList();

        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null || Strings.isNullOrEmpty(address)) return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : providerUrls.entrySet()) {
            for (URL url : e1.getValue().values()) {
                if (address.equals(url.getAddress())) {
                    ret.add(e1.getKey());
                    break;
                }
            }
        }

        return ret;
    }

    public List<String> findApplications() {
        List<String> ret = Lists.newArrayList();
        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null) return ret;

        providerUrls.forEach((key, urls) -> urls.forEach((k, url) -> {
            String app = url.getParameter(Constants.APPLICATION_KEY);
            if (!Strings.isNullOrEmpty(app))
                ret.add(app);
        }));
        return ret;
    }

    public List<Provider> findByApplication(String application) {
        return SyncUtils.url2ProviderList(findProviderUrlByApplication(application));
    }

    private Map<Long, URL> findProviderUrlByApplication(String application) {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(Constants.APPLICATION_KEY, application);
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<String> findServicesByApplication(String application) {
        List<String> ret = Lists.newArrayList();

        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null || Strings.isNullOrEmpty(application)) return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : providerUrls.entrySet()) {
            for (URL url : e1.getValue().values()) {
                if (application.equals(url.getParameter(Constants.APPLICATION_KEY))) {
                    ret.add(e1.getKey());
                    break;
                }
            }
        }

        return ret;
    }

    public List<String> findMethodsByService(String service) {
        List<String> ret = Lists.newArrayList();

        ConcurrentMap<String, Map<Long, URL>> providerUrls = getRegistryCache().get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null || Strings.isNullOrEmpty(service)) return ret;

        Map<Long, URL> providers = providerUrls.get(service);
        if (CollectionUtils.isEmpty(providers)) return ret;

        Entry<Long, URL> p = providers.entrySet().iterator().next();
        String value = p.getValue().getParameter("methods");
        if (Strings.isNullOrEmpty(value)) {
            return ret;
        }
        String[] methods = value.split(ParseUtils.METHOD_SPLIT);
        if (CollectionUtils.isEmpty(methods)) {
            return ret;
        }

        ret.addAll(Arrays.asList(methods));
        return ret;
    }

    private URL findProviderUrl(Long id) {
        return findProvider(id).toUrl();
    }

    public Provider findByServiceAndAddress(String service, String address) {
        return SyncUtils.url2Provider(findProviderUrl(address));
    }

    private Pair<Long, URL> findProviderUrl(String address) {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);

        Map<Long, URL> ret = SyncUtils.filterFromCategory(getRegistryCache(), filter);
        if (ret.isEmpty()) {
            return null;
        }
        Long key = ret.entrySet().iterator().next().getKey();
        return new Pair<>(key, ret.get(key));
    }

}
