package com.fyp.moviecommunity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the per-request performance interceptor into Spring MVC.
 * Anything else that needs an interceptor in future goes here too.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor perfInterceptor;

    public WebMvcConfig(PerformanceInterceptor perfInterceptor) {
        this.perfInterceptor = perfInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(perfInterceptor);
    }
}
