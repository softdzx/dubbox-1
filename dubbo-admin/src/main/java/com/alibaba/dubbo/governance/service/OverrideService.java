package com.alibaba.dubbo.governance.service;

import java.util.List;

import com.alibaba.dubbo.registry.common.domain.Override;

public interface OverrideService {
    
    void saveOverride(Override override);
    
    void updateOverride(Override override);

    void deleteOverride(Long id);
    
    void enableOverride(Long id);
    
    void disableOverride(Long id);

    List<Override> findByService(String service);
    
    List<Override> findByAddress(String address);
    
    List<Override> findByServiceAndAddress(String service, String address);
    
    List<Override> findByApplication(String application);

    List<Override> findByServiceAndApplication(String service, String application);
    
    List<Override> findAll();

    Override findById(Long id);
    
}
