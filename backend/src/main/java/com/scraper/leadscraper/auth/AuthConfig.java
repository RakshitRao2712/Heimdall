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
    @Bean SecurityFilterChain security(HttpSecurity http, HttpSessionSecurityContextRepository repository, org.springframework.core.env.Environment env, GoogleIdentityStore identities) throws Exception {
        String id = env.getProperty("GOOGLE_CLIENT_ID", "").trim(), secret = env.getProperty("GOOGLE_CLIENT_SECRET", "").trim();
        String frontend = env.getProperty("HEIMDALL_FRONTEND_URL", "http://127.0.0.1:5174");
        if (!id.isEmpty() && !secret.isEmpty()) {
            var registration = org.springframework.security.config.oauth2.client.CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(id).clientSecret(secret).scope("openid", "email", "profile")
                .redirectUri(env.getProperty("HEIMDALL_GOOGLE_REDIRECT_URI", frontend + "/api/auth/google/callback")).build();
            var clients = new org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository(registration);
            var authorizedClients = new org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService(clients);
            http.oauth2Login(o -> o.clientRegistrationRepository(clients)
                .authorizedClientService(authorizedClients)
                .authorizationEndpoint(a -> a.baseUri("/api/auth/google/authorize"))
                .redirectionEndpoint(r -> r.baseUri("/api/auth/google/callback"))
                .successHandler((request, response, authentication) -> {
                    try {
                        var user = (org.springframework.security.oauth2.core.oidc.user.OidcUser) authentication.getPrincipal();
                        authorizedClients.removeAuthorizedClient("google", authentication.getName());
                        String email = identities.signIn(user.getSubject(), user.getEmail(), Boolean.TRUE.equals(user.getEmailVerified()), user.getClaimAsString("hd"));
                        request.getSession().setMaxInactiveInterval(28800);
                        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
                        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
                        repository.saveContext(context, request, response);
                        request.getSession().removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
                        response.sendRedirect(frontend + "/login?google=success");
                    } catch (Exception failure) {
                        org.springframework.security.core.context.SecurityContextHolder.clearContext();
                        request.getSession().invalidate();
                        response.sendRedirect(frontend + "/login?google=error");
                    }
                }).failureHandler((request, response, failure) -> response.sendRedirect(frontend + "/login?google=error")));
        }
        return http
            .securityContext(c -> c.securityContextRepository(repository))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/google/config", "/api/auth/google/authorize/**", "/api/auth/google/callback", "/api/auth/csrf", "/api/auth/login", "/api/auth/register", "/api/auth/register/verify", "/api/auth/register/resend", "/error").permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
            .logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)).invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .build();
    }
}
