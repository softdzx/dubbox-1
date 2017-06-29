package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;

/**
 * RPC Result.
 *
 * @author qianlei
 * @serial Don't change the class name and properties.
 */
public class RpcResult implements Result, Serializable {

    private static final long serialVersionUID = -6925924956850004727L;

    private Object result;

    private Throwable exception;

    private Map<String, String> attachments = Maps.newHashMap();

    public RpcResult() {
    }

    public RpcResult(Object result) {
        this.result = result;
    }

    public RpcResult(Throwable exception) {
        this.exception = exception;
    }

    public Object recreate() throws Throwable {
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    /**
     * @see com.alibaba.dubbo.rpc.RpcResult#getValue()
     * @deprecated Replace to getValue()
     */
    @Deprecated
    public Object getResult() {
        return getValue();
    }

    /**
     * @see com.alibaba.dubbo.rpc.RpcResult#setResult(Object)
     * @deprecated Replace to setValue()
     */
    @Deprecated
    public void setResult(Object result) {
        setValue(result);
    }

    public Object getValue() {
        return result;
    }

    public void setValue(Object value) {
        this.result = value;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable e) {
        this.exception = e;
    }

    public boolean hasException() {
        return exception != null;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public String getAttachment(String key) {
        return attachments.get(key);
    }

    public String getAttachment(String key, String defaultValue) {
        String result = attachments.get(key);
        if (Strings.isNullOrEmpty(result)) {
            result = defaultValue;
        }
        return result;
    }

    public void setAttachments(Map<String, String> map) {
        if (!CollectionUtils.isEmpty(map)) {
            attachments.putAll(map);
        }
    }

    public void setAttachment(String key, String value) {
        attachments.put(key, value);
    }

    @Override
    public String toString() {
        return "RpcResult [result=" + result + ", exception=" + exception + "]";
    }
}