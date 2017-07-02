package com.alibaba.dubbo.config.support;

import java.lang.annotation.*;

/**
 * Parameter
 *
 * @author william.liangf
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {

    String key() default "";

    boolean required() default false;

    boolean excluded() default false;

    boolean escaped() default false;

    boolean attribute() default false;

    boolean append() default false;

}