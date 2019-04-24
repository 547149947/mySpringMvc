package com.kevin.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD) //注解只能修饰属性
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {

    String value() default "";
}
