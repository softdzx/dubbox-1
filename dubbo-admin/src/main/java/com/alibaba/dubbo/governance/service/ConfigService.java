package com.alibaba.dubbo.governance.service;

import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.registry.common.domain.Config;

/**
 * Comment of ConfigDAO
 * 
 * @author rain.chenjr
 * 
 */
public interface ConfigService {

	void update(List<Config> configs);

	Map<String, String> findAllConfigsMap();

}
