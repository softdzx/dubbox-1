package com.alibaba.dubbo.config;

import com.alibaba.dubbo.config.support.Parameter;

import java.io.Serializable;

/**
 * @author chao.liuc
 */
public class ArgumentConfig implements Serializable {

    private static final long serialVersionUID = -2165482463925213595L;

    /**
     * arugment index -1 represents not set
     */
    private Integer index = -1;

    /**
     * argument type
     */
    private String type;

    /**
     * callback interface
     */
    private Boolean callback;

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Parameter(excluded = true)
    public Integer getIndex() {
        return index;
    }

    @Parameter(excluded = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCallback(Boolean callback) {
        this.callback = callback;
    }

    public Boolean isCallback() {
        return callback;
    }

}