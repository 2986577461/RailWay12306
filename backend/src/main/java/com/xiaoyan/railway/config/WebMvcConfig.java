package com.xiaoyan.railway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link AuthInterceptor} on login-required paths only; all other paths
 * (query, register, login, provider callbacks, etc.) stay public.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/users/me",
                        "/api/passengers/**",
                        "/api/orders/**",
                        "/api/payments/**")
                // 支付回调靠验签鉴权，不靠登录态（模拟真实微信异步回调）
                .excludePathPatterns("/api/payments/*/mock-notify");
    }
}
