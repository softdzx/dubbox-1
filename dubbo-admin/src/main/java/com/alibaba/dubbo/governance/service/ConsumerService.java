package com.alibaba.dubbo.governance.service;

import java.util.List;

import com.alibaba.dubbo.registry.common.domain.Consumer;

/**
 * 消费者数据访问对象
 * 
 * @author william.liangf
 */
public interface ConsumerService {

    List<Consumer> findByService(String serviceName);

    Consumer findConsumer(Long id);
    
    List<Consumer> findAll();

    /**
     * 查询所有的消费者地址
     */
    List<String> findAddresses();
    
    List<String> findAddressesByApplication(String application);
    
    List<String> findAddressesByService(String serviceName);

    List<Consumer> findByAddress(String consumerAddress);
    
    List<String> findServicesByAddress(String consumerAddress);

    List<String> findApplications();
    
    List<String> findApplicationsByServiceName(String serviceName);
    
    List<Consumer> findByApplication(String application);
    
    List<String> findServicesByApplication(String application);

    List<String> findServices();
    
}