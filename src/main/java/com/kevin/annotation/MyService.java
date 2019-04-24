package com.kevin.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE) //注解只能修饰类、接口、注解类型或枚举类型
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {

    /**
     * Service 别名/自定义名
     *
     * @return
     */
    String value() default "";
}
