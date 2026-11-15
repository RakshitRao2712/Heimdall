package com.scraper.leadscraper.billing;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PremiumAccess implements WebMvcConfigurer {
 // Discovery is free, but it is never anonymous.
 static HandlerInterceptor authenticatedAccess() {
  return new HandlerInterceptor() {
   public boolean preHandle(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res, Object handler) throws Exception {
    if (req.getUserPrincipal() != null) return true;
    res.setStatus(401);
    res.setContentType("application/json");
    res.getWriter().write("{\"detail\":\"Sign in to use this search method.\"}");
    return false;
   }
  };
 }
 public void addInterceptors(InterceptorRegistry registry) {
  registry.addInterceptor(authenticatedAccess()).addPathPatterns("/api/workspace/search/**", "/api/workspace/browser-verification", "/api/workspace/browser-verification/**", "/api/lead", "/api/lead/**", "/api/leads/**", "/api/google-search/**");
 }
}
