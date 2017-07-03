package com.alibaba.dubbo.common.status;

import com.alibaba.dubbo.common.extension.SPI;

@SPI
public interface StatusChecker {
    
    /**
     * check status
     * 
     * @return status
     */
    Status check();

}