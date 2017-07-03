package com.alibaba.dubbo.container.page;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.SPI;

@SPI
public interface PageHandler {
    
    /**
     * Handle the page.
     * 
     * @param url
     * @return the page.
     */
    Page handle(URL url);

}