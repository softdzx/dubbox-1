package com.alibaba.dubbo.validation.support.jvalidation;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.validation.Validator;
import com.alibaba.dubbo.validation.support.AbstractValidation;

public class JValidation extends AbstractValidation {

    @Override
    protected Validator createValidator(URL url) {
        return new JValidator(url);
    }

}