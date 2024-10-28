package com.sti.accounting.config;


import com.sti.accounting.filters.TokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig  {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

//    @Bean
//    public FilterRegistrationBean<TokenFilter> tokenFilterRegistration() {
//
//        FilterRegistrationBean<TokenFilter> registrationBean = new FilterRegistrationBean<>();
//
//        registrationBean.setFilter(new TokenFilter());
//        registrationBean.addUrlPatterns("/*");
//        registrationBean.setName("TokenFilter");
//        registrationBean.setOrder(1);
//
//        return registrationBean;
//
//    }
}



