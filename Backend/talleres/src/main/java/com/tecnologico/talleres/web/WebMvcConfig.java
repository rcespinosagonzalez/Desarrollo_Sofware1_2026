package com.tecnologico.talleres.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Registro el interceptor de sesion en todas las rutas
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SessionRequiredInterceptor sessionRequiredInterceptor;

    public WebMvcConfig(SessionRequiredInterceptor sessionRequiredInterceptor) {
        this.sessionRequiredInterceptor = sessionRequiredInterceptor;
    }

    // Aplico el filtro de sesion a /** excepto rutas publicas del interceptor
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionRequiredInterceptor).addPathPatterns("/**");
    }
}
