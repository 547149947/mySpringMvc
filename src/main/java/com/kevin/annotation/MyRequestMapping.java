package com.kevin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD}) //注解只能修饰类、接口、注解类型或枚举类型，以及方法
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {

    /**
     * 访问该方法的url
     *
     * @return
     */
    String value() default "";
}
