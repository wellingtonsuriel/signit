package zw.co.telone.signit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    JwtUtil jwtUtil = new JwtUtil(); // Instantiate JwtUtil or obtain it from your dependency injection framework
    AuthInterceptor authInterceptor = new AuthInterceptor(jwtUtil);
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtil))
                .addPathPatterns("/api7/v1/**");
    }
}
