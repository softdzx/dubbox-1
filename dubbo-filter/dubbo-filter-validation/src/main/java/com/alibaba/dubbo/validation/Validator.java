package com.alibaba.dubbo.validation;

public interface Validator {

    void validate(String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception;

}
