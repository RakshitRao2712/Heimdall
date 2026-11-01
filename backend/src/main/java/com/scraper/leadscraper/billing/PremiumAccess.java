package com.scraper.leadscraper.billing;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
@Configuration
public class PremiumAccess implements WebMvcConfigurer {
 private final Billing billing;public PremiumAccess(Billing billing){this.billing=billing;}
 public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){public boolean preHandle(jakarta.servlet.http.HttpServletRequest req,jakarta.servlet.http.HttpServletResponse res,Object handler)throws Exception{if(req.getUserPrincipal()!=null&&billing.premium(req.getUserPrincipal().getName()))return true;res.setStatus(402);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Premium lifetime access is required to use this search method.\"}");return false;}}).addPathPatterns("/api/workspace/search/**","/api/lead", "/api/lead/**","/api/leads/**","/api/google-search/**");}
}
