package com.kevin.servlet;

import com.kevin.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyDispatcherServlet extends HttpServlet {
    /**
     * 属性-加载配置文件
     */
    private static Properties properties = new Properties();
    /**
     * 存放扫描的 class 名称
     */
    private static ArrayList<String> classNames = new ArrayList<>();
    /**
     * 注入的IOC容器
     */
    private static Map<String, Object> iocMap = new HashMap<>();

    /**
     * 控制器集合
     */
    private static Map<String, Object> controllerMap = new HashMap<>();
    /**
     * 请求地址映射-controller-方法
     */
    private static Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        logger("MyDispatcherServlet.init() start");
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        logger("MyDispatcherServlet.init() 加载配置文件完成");

        //扫描用户设定的包下面的所有类
        String scanPackage = properties.getProperty("scanPackage");
        doScanner(scanPackage);
        logger("MyDispatcherServlet.init() 扫描包文件完成");

        //将扫描到的类（controller、service类），通过反射机制进行实例化，然后，放入IOC容器中（beanName：bean）beanName首字母小写
        doInstance();
        logger("MyDispatcherServlet.init() 实例化控制器，服务完成");

        //http请求路径与Method建立映射关系
        doUrlMapping();
        logger("MyDispatcherServlet.init() 请求映射完成");

        //依赖注入，实现ioc机制
        doIoc();
        logger("MyDispatcherServlet.init() 依赖注入完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //处理请求
            resp.setHeader("Content-type", "text/html;charset=UTF-8");
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (handlerMapping.isEmpty()) {
            return;
        }

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String url = uri.replace(contextPath, "").replaceAll("/+", "/");
        if (!handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }

        Method method = handlerMapping.get(url);
        // 获取方法的参数列表
        Parameter[] parameters = method.getParameters();
        // 调用方法需要传递的形参
        Object[] paramValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (ServletRequest.class.isAssignableFrom(parameters[i].getType())) {
                paramValues[i] = req;
                continue;
            }
            if (ServletResponse.class.isAssignableFrom(parameters[i].getType())) {
                paramValues[i] = resp;
                continue;
            }

            /**
             * 其它参数，目前只支持String，Integer，Float，Double
             */
            String paramKey = parameters[i].getName();
            if (parameters[i].isAnnotationPresent(MyRequestParam.class)) {
                paramKey = parameters[i].getAnnotation(MyRequestParam.class).value();
            }
            //获取请求中的参数值
            String paramValue = req.getParameter(paramKey);
            paramValues[i] = paramValue;
            if (paramValue != null) {
                if (Integer.class.isAssignableFrom(parameters[i].getType())) {
                    paramValues[i] = Integer.parseInt(paramValue);
                } else if (Float.class.isAssignableFrom(parameters[i].getType())) {
                    paramValues[i] = Float.parseFloat(paramValue);
                } else if (Double.class.isAssignableFrom(parameters[i].getType())) {
                    paramValues[i] = Double.parseDouble(paramValue);
                }
            }
        }
        try {
            /**
             * 利用反射机制来执行接口方法
             */
            method.invoke(controllerMap.get(url), paramValues);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 加载配置文件信息
     *
     * @param location
     */
    private void doLoadConfig(String location) {
        if (location.startsWith("classpath:")) {
            location = location.substring(location.indexOf(":") + 1);
        }
        //加载 web.xml 中 contextConfigLocation 对应的配置文件信息 application.properties
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
            properties.load(resourceAsStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * 扫描包
     *
     * @param packageName
     */
    private void doScanner(String packageName) {
        //把所有的 . 替换成 /
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                //递归读取文件
                doScanner(packageName + "." + file.getName());
            } else {
                //获取类名：com.kevin.controller.HelloController 方便反射实例化
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 将扫描的类，实例化加入IOC容器中
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                /**
                 * 通过反射，进行实例化（只将加 @MyController、@MyService 注解的进行实例化）加入IOC容器中
                 * key设计：优先使用自定义名字，没有则使用类名（首字母小写）作为key
                 */
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    MyController myController = clazz.getAnnotation(MyController.class);
                    String key = myController.value().trim();
                    if ("".equals(key)) {
                        key = toLowerFirstWord(clazz.getSimpleName());
                    }
                    iocMap.put(key, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String key = myService.value().trim();
                    if ("".equals(key.trim())) {
                        key = toLowerFirstWord(key);
                    }
                    iocMap.put(key, clazz.newInstance());
                } else {
                    continue;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }
        }
    }

    /**
     * 依赖注入
     */
    private void doIoc() {
        if (iocMap.isEmpty()) {
            return;
        }
        //遍历所有注入类的属性，对 MyAutowired 的变量进行依赖注入
        for (Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String key = myAutowired.value().trim();
                if ("".equals(key)) {
                    key = toLowerFirstWord(field.getType().getSimpleName());
                }
                try {
                    //获取访问私有属性的权限
                    field.setAccessible(true);
                    //注入实例
                    field.set(entry.getValue(), iocMap.get(key));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * http请求路径与Method建立映射关系
     */
    private void doUrlMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        try {
            for (Entry<String, Object> entry : iocMap.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(MyController.class)) {
                    continue;
                }

                //拼接URL：controller 头的 url + 方法上的 url
                String baseUrl;
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    baseUrl = clazz.getAnnotation(MyRequestMapping.class).value();
                } else {
                    baseUrl = clazz.getAnnotation(MyController.class).value();
                }
                //遍历 controller 中的每个使用 @MyRequestMapping 的方法，拼接下一级请求路径
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                        continue;
                    }
                    MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                    String url = ("/" + baseUrl + "/" + myRequestMapping.value().trim()).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
                    controllerMap.put(url, entry.getValue());
                    logger("请求注入：" + url + "," + method);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 字符串的首字母小写
     *
     * @param name
     * @return
     */
    private String toLowerFirstWord(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    public void logger(String logInfo) {
        System.out.println(logInfo);
    }
}