package com.scraper.leadscraper.security;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
class ApiProtectionFilterTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void oversizedChunkedJsonIsRejectedBeforeController()throws Exception{var request=new MockHttpServletRequest("POST","/api/auth/login");request.setContentType("application/json");request.setContent(new byte[16385]);var response=new MockHttpServletResponse();new ApiProtectionFilter().doFilter(request,response,(a,b)->fail("Controller must not run"));assertEquals(413,response.getStatus());}
 @Test void bodyCanBeReadByControllerAndResponsesNeverCache()throws Exception{var request=new MockHttpServletRequest("POST","/api/auth/login");request.setContentType("application/json");request.setContent("{\"email\":\"x\"}".getBytes());var response=new MockHttpServletResponse();new ApiProtectionFilter().doFilter(request,response,(a,b)->assertEquals("{\"email\":\"x\"}",new String(a.getInputStream().readAllBytes())));assertEquals("no-store",response.getHeader("Cache-Control"));}
 @Test void accountJoinBudgetCannotBeBypassedByDifferentIpOrSession()throws Exception{var filter=new ApiProtectionFilter();SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("member",null,List.of()));for(int i=0;i<11;i++){var req=new MockHttpServletRequest("POST","/api/workspace/teams/join");req.setRemoteAddr("192.0.2."+i);var res=new MockHttpServletResponse();filter.doFilter(req,res,(a,b)->{});assertEquals(i<10?200:429,res.getStatus());}}
 @Test void expiredAbsoluteSessionIsInvalidated()throws Exception{var req=new MockHttpServletRequest("GET","/api/auth/me");var session=new MockHttpSession();session.setAttribute("heimdall.authenticatedAt",System.currentTimeMillis()-604_800_001L);req.setSession(session);var res=new MockHttpServletResponse();new ApiProtectionFilter().doFilter(req,res,(a,b)->fail());assertEquals(401,res.getStatus());assertTrue(session.isInvalid());}
}
