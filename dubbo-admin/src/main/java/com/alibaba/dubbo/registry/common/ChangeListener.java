package com.alibaba.dubbo.registry.common;

public interface ChangeListener {

	/**
	 * 数据变更
	 * 
	 * @param type 数据类型
	 */
	void onChanged(String type);

}
