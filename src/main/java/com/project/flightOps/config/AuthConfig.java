package com.project.flightOps.config;

import com.project.flightOps.service.UserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class AuthConfig {

    // Remove this bean entirely - let Spring use the @Component CustomPasswordEncoder
    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new CustomPasswordEncoder();
    // }

    @Bean
    public AuthenticationProvider authenticationProvider(UserServiceImpl userService, CustomPasswordEncoder passwordEncoder) {
        System.out.println("=== AuthConfig Debug ===");
        System.out.println("CustomPasswordEncoder instance: " + passwordEncoder.getClass().getSimpleName());

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider((UserDetailsService) userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
