package com.scraper.leadscraper.service;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

/** Isolated Chromium context; used only by the owning search thread. */
public final class PlaywrightGoogleDiscovery implements AutoCloseable {
 private static final Semaphore SLOT=new Semaphore(1);
 private Playwright playwright; private Browser browser; private Page page;
 private boolean acquired; private Runnable verificationHandler; private final boolean visible;
 private final VerificationNetworkHealth health=new VerificationNetworkHealth();
 boolean visible(){return visible;}
 void setVerificationHandler(Runnable handler){verificationHandler=handler;}
 PlaywrightGoogleDiscovery(){this(false);}
 PlaywrightGoogleDiscovery(boolean visible){
  this.visible=visible;
  if(!SLOT.tryAcquire())throw error(HttpStatus.SERVICE_UNAVAILABLE,"Browser search is busy. Retry shortly.");
  acquired=true;
  try{
   playwright=Playwright.create();
   browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(!visible));
   var context=browser.newContext(new Browser.NewContextOptions().setViewportSize(1280,900).setAcceptDownloads(false));
   page=context.newPage();page.setDefaultNavigationTimeout(45000);page.setDefaultTimeout(10000);
   page.onPageError(health::acceptBrowserMessage);
   page.onResponse(r->health.resourceFailure(r.url(),r.status(),false));
   page.onRequestFailed(r->{if(!"net::ERR_ABORTED".equals(r.failure()))health.resourceFailure(r.url(),0,true);});
   page.onConsoleMessage(m->{if("error".equals(m.type()))health.acceptBrowserMessage(m.text());});
  }catch(Exception e){close();throw error(HttpStatus.SERVICE_UNAVAILABLE,"Could not start Playwright. Install its Chromium browser on the server; see the browser setup guide.");}
 }
 static URI searchUri(String query,int page){return URI.create("https://www.google.com/search?q="+URLEncoder.encode(query,StandardCharsets.UTF_8)+"&start="+(page*10)+"&hl=en");}
 String page(String query,int number){
  try{
   try{page.navigate(searchUri(query,number).toString(),new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));}
   catch(TimeoutError e){throw error(HttpStatus.GATEWAY_TIMEOUT,"Google did not finish loading. Check your connection and retry.");}
   checkChallenge();
   var reject=page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Reject all").setExact(true));
   if(reject.count()>0)reject.first().click();
   page.waitForFunction("() => document.querySelector('h3, [role=heading]') || document.body.innerText.includes('did not match any documents') || location.pathname.startsWith('/sorry') || /unusual traffic|not a robot|automated queries/i.test(document.body.innerText)");
   checkChallenge();requireGooglePage();return (String) page.locator("html").evaluate("element => element.outerHTML");
  }catch(ResponseStatusException e){throw e;}catch(PlaywrightException e){
   if(page==null||page.isClosed())throw error(HttpStatus.CONFLICT,"The browser window was closed. Start a new search and keep it open until completion.");
   org.slf4j.LoggerFactory.getLogger(PlaywrightGoogleDiscovery.class)
     .warn("Google browser search failed: {}", e.getClass().getSimpleName());
   throw error(HttpStatus.BAD_GATEWAY,"Google browser search could not load results. Retry later.");
  }
 }
 private void checkChallenge(){if(GoogleSearchScraperService.isBlocked(page.url(),page.content()) && !verificationReady()){if(verificationHandler==null)throw error(HttpStatus.BAD_GATEWAY,"Google verification is required. Start this search from Heimdall to open manual verification.");verificationHandler.run();}}
 private void requireGooglePage(){String host=URI.create(page.url()).getHost();if(host==null||!(host.equals("google.com")||host.endsWith(".google.com")))throw error(HttpStatus.BAD_GATEWAY,"Verification left Google. Search stopped for safety; start a new search.");}
 void focusVerification(){requireGooglePage();page.bringToFront();}
 String verificationHealth(){page.waitForTimeout(1);return health.message();}
 void reloadVerification(){requireGooglePage();health.clear();try{page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));}catch(TimeoutError e){throw error(HttpStatus.GATEWAY_TIMEOUT,"Google verification could not finish reloading. Start a new search.");}}
 byte[] verificationScreenshot(){
  requireGooglePage();
  try{return page.screenshot(new Page.ScreenshotOptions().setTimeout(1500));}
  catch(TimeoutError e){return null;}
 }
 boolean verificationReady(){
  requireGooglePage();
  if(!URI.create(page.url()).getPath().equals("/search"))return false;
  try{
   return Boolean.TRUE.equals(page.evaluate("""
     () => {
       const visible = element => element.getClientRects().length > 0;
       if ([...document.querySelectorAll('form#captcha-form')].some(visible)) return false;
       return [...document.querySelectorAll('a[href] h3, a[href] [role=heading]')].some(visible)
         || document.body.innerText.includes('did not match any documents');
     }
     """));
  }catch(PlaywrightException e){
   if(page.isClosed())throw e;
   return false;
  }
 }
 void manualInput(BrowserVerificationController.Input input){requireGooglePage();dispatchManualInput(page,input);}
 static void dispatchManualInput(Page page,BrowserVerificationController.Input input){switch(input.kind()){
  case "click"->{var size=page.viewportSize();page.mouse().click(Math.min(size.width-1,input.x()*size.width),Math.min(size.height-1,input.y()*size.height));}
  case "text"->page.keyboard().insertText(input.text());
  case "key"->page.keyboard().press(switch(input.text()){case "TAB"->"Tab";case "ENTER"->"Enter";case "SPACE"->"Space";case "BACKSPACE"->"Backspace";case "ARROWUP"->"ArrowUp";case "ARROWDOWN"->"ArrowDown";case "ARROWLEFT"->"ArrowLeft";default->"ArrowRight";});
  case "scroll"->page.mouse().wheel(0,input.y());default->throw new IllegalArgumentException("Invalid manual input");}}
 private static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
 public void close(){try{if(browser!=null)browser.close();}catch(Exception ignored){}finally{try{if(playwright!=null)playwright.close();}catch(Exception ignored){}finally{browser=null;page=null;playwright=null;if(acquired){acquired=false;SLOT.release();}}}}
}
