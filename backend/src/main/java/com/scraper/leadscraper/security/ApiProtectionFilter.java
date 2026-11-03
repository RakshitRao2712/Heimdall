package com.scraper.leadscraper.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Single-instance bounded abuse control. Never trust a caller-supplied forwarding header. */
public final class ApiProtectionFilter extends OncePerRequestFilter {
    private record Window(long since,int count){}
    private final Map<String,Window> windows=new HashMap<>();
    private synchronized boolean allowed(String key,int limit,long duration){
        long now=System.currentTimeMillis();windows.entrySet().removeIf(e->now-e.getValue().since>3_600_000);
        var old=windows.get(key);if(old==null||now-old.since>=duration){if(windows.size()>=20000)return false;windows.put(key,new Window(now,1));return true;}
        if(old.count>=limit)return false;windows.put(key,new Window(old.since,old.count+1));return true;
    }
    private void reject(HttpServletResponse response,int status,String detail)throws IOException{response.setStatus(status);response.setContentType("application/json");if(status==429)response.setHeader("Retry-After","900");response.getWriter().write("{\"detail\":\""+detail+"\"}");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        if(!request.getRequestURI().startsWith("/api/")){chain.doFilter(request,response);return;}
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
        var session=request.getSession(false);
        if(session!=null&&session.getAttribute("heimdall.authenticatedAt") instanceof Long started&&System.currentTimeMillis()-started>604_800_000L){session.invalidate();SecurityContextHolder.clearContext();reject(response,401,"Please sign in again.");return;}
        String path=request.getRequestURI();String ip=request.getRemoteAddr();
        if (Set.of("/api/workspace/search/copyright", "/api/workspace/search/serp",
                "/api/workspace/search/browser", "/api/workspace/search/yelp",
                "/api/workspace/brave-key", "/api/workspace/serp-key", "/api/workspace/yelp-key",
                "/api/lead/google-search").contains(path)
                || path.equals("/api/workspace/browser-verification")
                || path.startsWith("/api/workspace/browser-verification/")) {
            reject(response, 404, "This discovery method has been retired.");
            return;
        }
        if(path.startsWith("/api/auth/")&&!allowed("auth:"+ip,120,60_000)){reject(response,429,"Too many authentication requests. Please wait.");return;}
        var auth=SecurityContextHolder.getContext().getAuthentication();String owner=auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)?auth.getName():null;
        boolean search=path.equals("/api/workspace/search/maps")||path.equals("/api/workspace/search/copyright")||path.equals("/api/workspace/search/serp")||path.equals("/api/workspace/search/browser")||path.equals("/api/workspace/search/yelp")||path.equals("/api/workspace/search/foursquare")||path.equals("/api/lead")||path.startsWith("/api/lead/");
        if(owner!=null&&"POST".equals(request.getMethod())&&search&&!allowed("search:"+owner,12,3_600_000)){reject(response,429,"Search limit reached. Please try again later.");return;}
        if(owner!=null&&path.equals("/api/workspace/teams/join")&&"POST".equals(request.getMethod())&&!allowed("join:"+owner,10,900_000)){reject(response,429,"Too many team join attempts. Please wait.");return;}
        if(owner!=null&&!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod())&&!allowed("write:"+owner,120,60_000)){reject(response,429,"Too many changes. Please wait.");return;}
        if(request.getContentLengthLong()>16384){reject(response,413,"Request body is too large.");return;}
        if(!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod())&&request.getContentType()!=null&&request.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json")){
            byte[] body=request.getInputStream().readNBytes(16385);if(body.length>16384){reject(response,413,"Request body is too large.");return;}
            var wrapped=new HttpServletRequestWrapper(request){
                @Override public ServletInputStream getInputStream(){var in=new ByteArrayInputStream(body);return new ServletInputStream(){public int read(){return in.read();}public boolean isFinished(){return in.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};}
                @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
            };chain.doFilter(wrapped,response);return;
        }
        chain.doFilter(request,response);
    }
}
