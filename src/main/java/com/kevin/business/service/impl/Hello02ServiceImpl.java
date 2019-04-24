package com.kevin.business.service.impl;

import com.kevin.annotation.MyService;
import com.kevin.business.service.HelloService;

@MyService("hello02ServiceImpl")
public class Hello02ServiceImpl implements HelloService {

    @Override
    public String getHelloInfo(String str) {
        return "hello02 " + str;
    }
}
