package com.legakrishi.solar.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class WebPerfConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        var frb = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        frb.addUrlPatterns("/admin/charts/*", "/partners/charts/*");
        frb.setName("etagFilter");
        frb.setOrder(1);
        return frb;
    }
}
