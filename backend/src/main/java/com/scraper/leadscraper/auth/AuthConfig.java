package com.scraper.leadscraper.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class AuthConfig {
    @Bean org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use Heimdall sign-in"); };
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean HttpSessionSecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, HttpSessionSecurityContextRepository repository) throws Exception {
        return http
            .securityContext(c -> c.securityContextRepository(repository))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/register", "/error").permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
            .logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)).invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .build();
    }
}
