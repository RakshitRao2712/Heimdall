package com.scraper.leadscraper.security;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
class SafeWebsiteFetcherTest {
 @Test void blocksPrivateSpecialAndMappedAddresses()throws Exception{
  for(String host:new String[]{"0.0.0.0","127.0.0.1","10.1.2.3","172.16.1.1","192.168.0.1","169.254.169.254","100.100.100.200","198.18.0.1","224.0.0.1","255.255.255.255","::","::1","fe80::1","fc00::1","::ffff:127.0.0.1","2001:db8::1","2002:7f00:1::"})assertFalse(SafeWebsiteFetcher.isPublic(InetAddress.getByName(host)),host);
  assertTrue(SafeWebsiteFetcher.isPublic(InetAddress.getByName("8.8.8.8")));assertTrue(SafeWebsiteFetcher.isPublic(InetAddress.getByName("2606:4700:4700::1111")));
 }
 @Test void rejectsMixedDnsAnswersAndUnsafeUrls()throws Exception{
  assertThrows(UnknownHostException.class,()->SafeWebsiteFetcher.publicAddresses(new InetAddress[]{InetAddress.getByName("8.8.8.8"),InetAddress.getByName("127.0.0.1")}));
  for(String url:new String[]{"http://127.0.0.1/","http://[::1]/","http://2130706433/","http://localhost/","http://x.internal/","http://user:pass@example.com/","http://example.com:8080/","file:///etc/passwd","gopher://example.com/"})assertThrows(java.io.IOException.class,()->SafeWebsiteFetcher.validate(URI.create(url)),url);
 }
 @Test void cannotConnectToLoopbackEvenIfItIsListening()throws Exception{
  var server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var hits=new java.util.concurrent.atomic.AtomicInteger();server.createContext("/",e->{hits.incrementAndGet();e.sendResponseHeaders(200,0);e.close();});server.start();
  try(var fetcher=new SafeWebsiteFetcher(Duration.ofSeconds(2))){assertThrows(java.io.IOException.class,()->fetcher.fetch(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/")));assertEquals(0,hits.get());}finally{server.stop(0);}
 }
}
