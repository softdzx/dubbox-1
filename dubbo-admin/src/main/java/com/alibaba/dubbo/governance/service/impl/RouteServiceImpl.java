package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.sync.util.Pair;
import com.alibaba.dubbo.governance.sync.util.SyncUtils;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class RouteServiceImpl extends AbstractService implements RouteService {

    public void createRoute(Route route) {
        registryService.register(route.toUrl());
    }

    public void updateRoute(Route route) {
        Long id = route.getId();
        if (id == null) {
            throw new IllegalStateException("no route id");
        }
        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }

        registryService.unregister(oldRoute);
        registryService.register(route.toUrl());
    }

    public void deleteRoute(Long id) {
        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        registryService.unregister(oldRoute);
    }

    public void enableRoute(Long id) {
        if (id == null) {
            throw new IllegalStateException("no route id");
        }

        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        if (oldRoute.getParameter("enabled", true)) {
            return;
        }

        registryService.unregister(oldRoute);
        URL newRoute = oldRoute.addParameter("enabled", true);
        registryService.register(newRoute);

    }

    public void disableRoute(Long id) {
        if (id == null) {
            throw new IllegalStateException("no route id");
        }

        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        if (!oldRoute.getParameter("enabled", true)) {
            return;
        }

        URL newRoute = oldRoute.addParameter("enabled", false);
        registryService.unregister(oldRoute);
        registryService.register(newRoute);

    }

    public List<Route> findAll() {
        return SyncUtils.url2RouteList(findAllUrl());
    }

    private Map<Long, URL> findAllUrl() {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public Route findRoute(Long id) {
        return SyncUtils.url2Route(findRouteUrlPair(id));
    }

    public Pair<Long, URL> findRouteUrlPair(Long id) {
        return SyncUtils.filterFromCategory(getRegistryCache(), Constants.ROUTERS_CATEGORY, id);
    }

    private URL findRouteUrl(Long id) {
        return findRoute(id).toUrl();
    }

    private Map<Long, URL> findRouteUrl(String service, String address, boolean force) {
        Map<String, String> filter = Maps.newHashMap();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        if (!Strings.isNullOrEmpty(service)) {
            filter.put(SyncUtils.SERVICE_FILTER_KEY, service);
        }
        if (!Strings.isNullOrEmpty(address)) {
            filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);
        }
        if (force) {
            filter.put("force", "true");
        }
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Route> findByService(String serviceName) {
        return SyncUtils.url2RouteList(findRouteUrl(serviceName, null, false));
    }

    public List<Route> findByAddress(String address) {
        return SyncUtils.url2RouteList(findRouteUrl(null, address, false));
    }

    public List<Route> findByServiceAndAddress(String service, String address) {
        return SyncUtils.url2RouteList(findRouteUrl(service, address, false));
    }

    public List<Route> findForceRouteByService(String service) {
        return SyncUtils.url2RouteList(findRouteUrl(service, null, true));
    }

    public List<Route> findForceRouteByAddress(String address) {
        return SyncUtils.url2RouteList(findRouteUrl(null, address, true));
    }

    public List<Route> findForceRouteByServiceAndAddress(String service, String address) {
        return SyncUtils.url2RouteList(findRouteUrl(service, address, true));
    }

    public List<Route> findAllForceRoute() {
        return SyncUtils.url2RouteList(findRouteUrl(null, null, true));
    }

}
