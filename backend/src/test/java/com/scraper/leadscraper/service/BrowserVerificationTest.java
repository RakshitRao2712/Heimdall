package com.scraper.leadscraper.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BrowserVerificationTest {
 @Test void manualSessionIsIsolatedAndResumesOnlyWhenGoogleIsReady() throws Exception {
  var browser=mock(PlaywrightGoogleDiscovery.class);var captured=new CountDownLatch(1);
  when(browser.verificationScreenshot()).thenAnswer(i->{captured.countDown();return new byte[]{1,2,3};});when(browser.verificationReady()).thenReturn(false,true);
  var controller=new BrowserVerificationController();var owner=new MockHttpSession();var other=new MockHttpSession();var cancelled=new AtomicBoolean();var executor=Executors.newSingleThreadExecutor();
  var task=executor.submit(()->BrowserVerificationController.await(owner,browser,cancelled::get));
  try {
   assertTrue(captured.await(2,TimeUnit.SECONDS));var state=controller.status(owner);assertTrue(state.waiting());assertFalse(state.localWindow());assertFalse(controller.status(other).waiting());assertThrows(ResponseStatusException.class,()->controller.focus(new BrowserVerificationController.Resume(state.challengeId()),owner));assertThrows(ResponseStatusException.class,()->controller.focus(new BrowserVerificationController.Resume(state.challengeId()),other));
   assertThrows(ResponseStatusException.class,()->controller.frame(state.challengeId(),other));assertThrows(ResponseStatusException.class,()->controller.resume(new BrowserVerificationController.Resume(state.challengeId()),other));
   assertThrows(ResponseStatusException.class,()->controller.input(new BrowserVerificationController.Input(state.challengeId(),"click",.2,.3,null),other));
   assertArrayEquals(new byte[]{1,2,3},controller.frame(state.challengeId(),owner).getBody());
   assertThrows(ResponseStatusException.class,()->controller.reload(new BrowserVerificationController.Resume(state.challengeId()),other));
   controller.reload(new BrowserVerificationController.Resume(state.challengeId()),owner);assertThrows(ResponseStatusException.class,()->controller.reload(new BrowserVerificationController.Resume(state.challengeId()),owner));
   controller.input(new BrowserVerificationController.Input(state.challengeId(),"click",.2,.3,null),owner);
   controller.resume(new BrowserVerificationController.Resume(state.challengeId()),owner);
   Thread.sleep(850);assertFalse(task.isDone());assertTrue(controller.status(owner).message().contains("Waiting for Google"));assertEquals(1,controller.status(owner).appliedInputs());
   task.get(2,TimeUnit.SECONDS);
   verify(browser).manualInput(any());verify(browser).reloadVerification();verify(browser,never()).focusVerification();assertFalse(controller.status(owner).waiting());assertThrows(ResponseStatusException.class,()->controller.frame(state.challengeId(),owner));
  }finally{cancelled.set(true);executor.shutdownNow();}
 }
 @Test void cancellationAndExpiryCleanUpThePrivateView() throws Exception {
  var browser=mock(PlaywrightGoogleDiscovery.class);when(browser.verificationScreenshot()).thenReturn(new byte[]{1});var controller=new BrowserVerificationController();var session=new MockHttpSession();
  var cancelled=assertThrows(ResponseStatusException.class,()->BrowserVerificationController.await(session,browser,()->true));assertTrue(cancelled.getReason().contains("cancelled"));assertFalse(controller.status(session).waiting());
  var expired=assertThrows(ResponseStatusException.class,()->BrowserVerificationController.await(session,browser,()->false,Duration.ZERO));assertEquals(408,expired.getStatusCode().value());assertFalse(controller.status(session).waiting());
 }
 @Test void rejectsArbitraryCommandsUnsafeCoordinatesAndWebDriverSpecialKeys() {
  for(var input:java.util.List.of(
   new BrowserVerificationController.Input("id","navigate",null,null,"http://localhost"),
   new BrowserVerificationController.Input("id","click",Double.NaN,.5,null),
   new BrowserVerificationController.Input("id","click",1.0,.5,null),
   new BrowserVerificationController.Input("id","text",null,null,"\ue009l"),
   new BrowserVerificationController.Input("id","text",null,null,"bad\ntext"),
   new BrowserVerificationController.Input("id","key",null,null,"CONTROL"),
   new BrowserVerificationController.Input("id","scroll",null,100000.0,null)
  ))assertThrows(ResponseStatusException.class,()->BrowserVerificationController.validate(input));
  assertDoesNotThrow(()->BrowserVerificationController.validate(new BrowserVerificationController.Input("id","text",null,null,"answer123")));
 }
 @Test void visibleBrowserStreamsInlineWithoutAutomaticallyFocusingChrome() throws Exception {
  var browser=mock(PlaywrightGoogleDiscovery.class);when(browser.visible()).thenReturn(true);when(browser.verificationReady()).thenReturn(true);
  var captured=new CountDownLatch(1);when(browser.verificationScreenshot()).thenAnswer(i->{captured.countDown();return new byte[]{4,5,6};});
  var controller=new BrowserVerificationController();var owner=new MockHttpSession();var other=new MockHttpSession();var cancelled=new AtomicBoolean();var executor=Executors.newSingleThreadExecutor();
  var task=executor.submit(()->BrowserVerificationController.await(owner,browser,cancelled::get));
  try {
   assertTrue(captured.await(2,TimeUnit.SECONDS));var status=controller.status(owner);assertTrue(status.localWindow());
   assertThrows(ResponseStatusException.class,()->controller.focus(new BrowserVerificationController.Resume(status.challengeId()),other));
   assertArrayEquals(new byte[]{4,5,6},controller.frame(status.challengeId(),owner).getBody());
   verify(browser,never()).focusVerification();
   controller.focus(new BrowserVerificationController.Resume(status.challengeId()),owner);controller.resume(new BrowserVerificationController.Resume(status.challengeId()),owner);task.get(2,TimeUnit.SECONDS);
   verify(browser).focusVerification();verify(browser,atLeastOnce()).verificationScreenshot();assertFalse(controller.status(owner).waiting());
  } finally {cancelled.set(true);executor.shutdownNow();}
 }
}
