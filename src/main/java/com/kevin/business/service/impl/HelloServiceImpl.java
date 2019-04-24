package com.kevin.business.service.impl;

import com.kevin.annotation.MyService;
import com.kevin.business.service.HelloService;

@MyService("helloServiceImpl")
public class HelloServiceImpl implements HelloService {

    @Override
    public String getHelloInfo(String str) {
        return "hello " + str;
    }
}
