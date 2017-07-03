package com.alibaba.dubbo.validation;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

@SPI("jvalidation")
public interface Validation {

    @Adaptive(Constants.VALIDATION_KEY)
    Validator getValidator(URL url);

}
