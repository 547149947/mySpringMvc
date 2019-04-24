package com.kevin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) //注解只能修饰方法定义的参数
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {

    /**
     * 参数别名，必填
     *
     * @return
     */
    String value();
}
