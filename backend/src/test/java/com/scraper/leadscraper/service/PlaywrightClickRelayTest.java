package com.scraper.leadscraper.service;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
class PlaywrightClickRelayTest {
 @Test @EnabledIfEnvironmentVariable(named="HEIMDALL_BROWSER_RELAY_TEST",matches="true")
 void forwardsCoordinatesIntoIframeAtBothPixelDensities(){
  try(var pw=Playwright.create();var browser=pw.chromium().launch()){
   for(double scale:new double[]{1,2})try(var context=browser.newContext(new Browser.NewContextOptions().setViewportSize(1280,900).setDeviceScaleFactor(scale))){
    var page=context.newPage();
    page.setContent("<iframe style='position:absolute;left:100px;top:100px;width:400px;height:200px;border:0' srcdoc=\"<button style='position:absolute;left:20px;top:20px;width:120px;height:60px' onclick=&quot;this.textContent='Clicked'&quot;>Click</button>\"></iframe>");
    page.frameLocator("iframe").locator("button").waitFor();
    PlaywrightGoogleDiscovery.dispatchManualInput(page,new BrowserVerificationController.Input("fixture","click",180.0/1280,150.0/900,null));
    assertEquals("Clicked",page.frameLocator("iframe").locator("button").textContent());
   }
  }
 }
}
