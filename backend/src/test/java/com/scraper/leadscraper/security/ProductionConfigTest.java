package com.scraper.leadscraper.security;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class ProductionConfigTest {
 @Test void onlyLoopbackProxyIsTrustedAndCookiesRequireTls()throws Exception{
  var config=new Properties();try(var in=getClass().getResourceAsStream("/application-production.properties")){config.load(in);}
  assertEquals("true",config.getProperty("server.servlet.session.cookie.secure"));assertEquals("127.0.0.1",config.getProperty("server.address"));
  var regex=config.getProperty("server.tomcat.remoteip.internal-proxies");assertTrue("127.0.0.1".matches(regex));assertTrue("::1".matches(regex));assertFalse("192.168.1.1".matches(regex));assertFalse("127a0b0c1".matches(regex));
 }
}
