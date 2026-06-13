package com.platform.agent.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext ctx;
    @Override
    public void setApplicationContext(ApplicationContext c) throws BeansException { ctx = c; }
    public static Object getBean(String name) { return ctx.getBean(name); }

    public static String invoke(Object bean, String method, Object arg) throws Exception {
        Method m = bean.getClass().getMethod(method, arg.getClass());
        Object out = m.invoke(bean, arg);
        return out == null ? "" : out.toString();
    }
}
