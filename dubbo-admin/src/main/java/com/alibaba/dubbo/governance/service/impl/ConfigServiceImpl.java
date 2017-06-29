package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.governance.service.ConfigService;
import com.alibaba.dubbo.registry.common.domain.Config;

import java.util.List;
import java.util.Map;

public class ConfigServiceImpl extends AbstractService implements ConfigService {

    /* (non-Javadoc)
     * @see com.alibaba.dubbo.governance.service.ConfigService#update(java.util.List)
     */
    public void update(List<Config> configs) {

    }

    /* (non-Javadoc)
     * @see com.alibaba.dubbo.governance.service.ConfigService#findAllConfigsMap()
     */
    public Map<String, String> findAllConfigsMap() {
        return null;
    }
}
