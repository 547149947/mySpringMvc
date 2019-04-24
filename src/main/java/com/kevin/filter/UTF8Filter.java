package com.kevin.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class UTF8Filter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String initParameter = filterConfig.getInitParameter("encoding");
        if (initParameter != null) {
            this.encoding = initParameter;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        servletRequest.setCharacterEncoding(encoding);
        filterChain.doFilter(servletRequest, servletResponse);
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        resp.setCharacterEncoding(encoding);
    }

    @Override
    public void destroy() {
    }
}
