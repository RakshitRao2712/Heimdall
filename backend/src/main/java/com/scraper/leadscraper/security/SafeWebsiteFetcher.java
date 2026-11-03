package com.scraper.leadscraper.security;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

/** Reject private destinations at socket DNS resolution; never use an automatic redirect or system proxy. */
public final class SafeWebsiteFetcher implements AutoCloseable {
    public static final int MAX_BYTES=1_048_576;
    private final org.apache.hc.client5.http.impl.classic.CloseableHttpClient client;
    private final ScheduledExecutorService deadlines=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"website-deadlines");t.setDaemon(true);return t;});
    private final long timeoutMillis;
    public SafeWebsiteFetcher(Duration timeout) {
        timeoutMillis=timeout.toMillis();
        var resolver=new DnsResolver(){
            public InetAddress[] resolve(String host)throws UnknownHostException{return publicAddresses(InetAddress.getAllByName(host));}
            public String resolveCanonicalHostname(String host)throws UnknownHostException{resolve(host);return host;}
        };
        var manager=PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver).setMaxConnTotal(32).setMaxConnPerRoute(4).build();
        client=HttpClients.custom().setConnectionManager(manager).disableRedirectHandling().disableCookieManagement().disableAuthCaching()
            .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofMilliseconds(timeoutMillis)).setConnectTimeout(Timeout.ofMilliseconds(timeoutMillis)).setResponseTimeout(Timeout.ofMilliseconds(timeoutMillis)).build()).build();
    }
    public static InetAddress[] publicAddresses(InetAddress[] addresses)throws UnknownHostException {
        if(addresses.length==0)throw new UnknownHostException("No public address");
        for(var a:addresses)if(!isPublic(a))throw new UnknownHostException("Non-public destination blocked");
        return addresses;
    }
    public static boolean isPublic(InetAddress a){
        if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())return false;
        byte[] v=a.getAddress();int x=v[0]&255,y=v[1]&255;
        if(v.length==4)return x!=0&&x!=10&&x!=127&&x<224&&!(x==100&&y>=64&&y<=127)&&!(x==169&&y==254)&&!(x==172&&y>=16&&y<=31)&&!(x==192&&(y==168||y==0||y==2))&&!(x==198&&(y==18||y==19||y==51))&&!(x==203&&y==0&&(v[2]&255)==113);
        // Public IPv6 global unicast only; block transition, documentation and special assignments.
        return (x&0xe0)==0x20&&!(x==0x20&&y==0x01&&(v[2]&255)<2)&&!(x==0x20&&y==0x01&&(v[2]&255)==0x0d&&(v[3]&255)==0xb8)&&!(x==0x20&&y==0x02)&&!(x==0x3f&&y==0xff);
    }
    public static void validate(URI uri)throws IOException {
        if(uri.toString().length()>2048||uri.getHost()==null||uri.getUserInfo()!=null||!("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme()))||!(uri.getPort()==-1||("http".equalsIgnoreCase(uri.getScheme())&&uri.getPort()==80)||("https".equalsIgnoreCase(uri.getScheme())&&uri.getPort()==443)))throw new IOException("Unsupported website URL");
        String host=uri.getHost().toLowerCase(Locale.ROOT);
        if(host.equals("localhost")||host.endsWith(".localhost")||host.endsWith(".local")||host.endsWith(".internal"))throw new IOException("Non-public host");
        // HttpClient may skip its DNS resolver for numeric literals, so check those explicitly.
        if(host.contains(":")||host.matches("[0-9.]+"))publicAddresses(InetAddress.getAllByName(host));
    }
    private record Page(int status,String location,String html){}
    public Document fetch(URI initial)throws IOException {
        URI current=initial;long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        for(int hop=0;hop<5;hop++){
            validate(current);long remaining=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(remaining<=0)throw new IOException("Website timeout");
            var request=new HttpGet(current);request.setHeader("User-Agent","HeimdallBot/1.0 (business website checks)");request.setHeader("Accept","text/html,application/xhtml+xml");
            var task=deadlines.schedule(()->request.cancel(),remaining,TimeUnit.MILLISECONDS);
            Page page;
            try{page=client.execute(request,response->{int status=response.getCode();String location=response.getFirstHeader("Location")==null?null:response.getFirstHeader("Location").getValue();
                if(status>=300&&status<400)return new Page(status,location,null);
                var entity=response.getEntity();if(status!=200||entity==null)return new Page(status,null,null);
                String type=entity.getContentType();if(type==null||!(type.toLowerCase(Locale.ROOT).startsWith("text/html")||type.toLowerCase(Locale.ROOT).startsWith("application/xhtml+xml")))return new Page(status,null,null);
                if(entity.getContentLength()>MAX_BYTES)throw new IOException("Website response too large");
                try(var stream=entity.getContent()){byte[] bytes=stream.readNBytes(MAX_BYTES+1);if(bytes.length>MAX_BYTES)throw new IOException("Website response too large");return new Page(status,null,new String(bytes,StandardCharsets.UTF_8));}
            });}finally{task.cancel(false);}
            if(page.status>=300&&page.status<400&&page.location!=null){URI next;try{next=current.resolve(page.location);}catch(IllegalArgumentException e){throw new IOException("Invalid redirect");}if("https".equalsIgnoreCase(current.getScheme())&&!"https".equalsIgnoreCase(next.getScheme()))throw new IOException("Insecure redirect");current=next;continue;}
            return page.html==null?null:Jsoup.parse(page.html,current.toString());
        }
        throw new IOException("Too many redirects");
    }
    public void close(){deadlines.shutdownNow();try{client.close();}catch(IOException ignored){}}
}
