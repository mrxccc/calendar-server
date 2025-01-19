package com.calendar.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;

import java.io.IOException;

@WebFilter(filterName = "MiltonFilter", urlPatterns = "/*",
        initParams = {
        @WebInitParam(name = "resource.factory.class",value = "io.milton.http.annotated.AnnotationResourceFactory")
        , @WebInitParam(name = "controllerPackagesToScan",value = "com.calendar.hellocaldav")})
public class MyFilter extends io.milton.servlet.MiltonFilter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain fc) throws IOException, ServletException {
        System.out.println("hello,myCaldavFilter!");
        super.doFilter(req, resp, fc);
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        System.out.println("hello,myCaldavFilter init!");
        super.init(config);
    }
}