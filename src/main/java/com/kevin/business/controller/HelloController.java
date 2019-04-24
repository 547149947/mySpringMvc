package com.kevin.business.controller;

import com.kevin.annotation.MyAutowired;
import com.kevin.annotation.MyController;
import com.kevin.annotation.MyRequestMapping;
import com.kevin.annotation.MyRequestParam;
import com.kevin.business.service.HelloService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MyController
@MyRequestMapping("/hello")
public class HelloController {

    @MyAutowired("helloServiceImpl")
    public HelloService helloServiceImpl;


    @MyAutowired("hello02ServiceImpl")
    public HelloService hello02ServiceImpl;


    @MyRequestMapping("/mySpringMvc")
    public void mySpringMvc(HttpServletRequest req, HttpServletResponse res, @MyRequestParam("param") String param) {
        try {
            String result = helloServiceImpl.getHelloInfo(param);
            res.getWriter().write("This is mySpringMvc execute! result:" + result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @MyRequestMapping("/mySpringMvc02")
    public void mySpringMvc02(HttpServletRequest req, HttpServletResponse res, @MyRequestParam("param") String param) {
        try {
            String result = hello02ServiceImpl.getHelloInfo(param);
            res.getWriter().write("This is mySpringMvc execute! result:" + result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
