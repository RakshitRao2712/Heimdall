package com.scraper.leadscraper.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VerificationNetworkHealthTest {
 String event(String method,String params){return "{\"message\":{\"method\":\""+method+"\",\"params\":"+params+"}}";}
 @Test void reportsChallengeNetworkFailuresWithoutExposingTokens(){
  var health=new VerificationNetworkHealth();
  health.accept(event("Network.requestWillBeSent","{\"requestId\":\"1\",\"request\":{\"url\":\"https://www.google.com/recaptcha/api2/anchor?token=private-secret\"}}"));
  health.accept(event("Network.loadingFailed","{\"requestId\":\"1\",\"errorText\":\"net::ERR_CONNECTION_TIMED_OUT\",\"canceled\":false}"));
  assertTrue(health.message().contains("failed to load"));assertFalse(health.message().contains("private-secret"));health.clear();assertTrue(health.message().startsWith("No reCAPTCHA"));
 }
 @Test void distinguishesProviderDenialsRateLimitsAndFailures(){
  for(int status:new int[]{403,429,503}) {
   var health=new VerificationNetworkHealth();health.accept(event("Network.responseReceived","{\"response\":{\"status\":"+status+",\"url\":\"https://www.google.com/recaptcha/api2/reload?k=private-key\"}}"));
   assertFalse(health.message().startsWith("No reCAPTCHA"));assertFalse(health.message().contains("private-key"));
  }
 }
 @Test void ordinarySearchBlockAndCancelledNavigationAreNotMisreportedAsBrokenCaptcha(){
  var health=new VerificationNetworkHealth();health.accept(event("Network.responseReceived","{\"response\":{\"status\":429,\"url\":\"https://www.google.com/search?q=test\"}}"));
  health.accept(event("Network.requestWillBeSent","{\"requestId\":\"1\",\"request\":{\"url\":\"https://www.google.com/recaptcha/api2/anchor\"}}"));
  health.accept(event("Network.loadingFailed","{\"requestId\":\"1\",\"errorText\":\"net::ERR_ABORTED\",\"canceled\":true}"));health.accept("not json");
  assertTrue(health.message().startsWith("No reCAPTCHA"));
 }
 @Test void browserScriptFailuresAndTimeoutsDoNotReportHealthyResources(){
  var health=new VerificationNetworkHealth();health.acceptBrowserMessage("Uncaught ReferenceError: solveSimpleChallenge is not defined");assertTrue(health.message().contains("JavaScript error"));
  health.acceptBrowserMessage("Uncaught (in promise) Error: reCAPTCHA Timeout");assertTrue(health.message().contains("timed out"));assertFalse(health.message().contains("No reCAPTCHA"));
  health.clear();assertTrue(health.message().startsWith("No reCAPTCHA"));
 }
}
