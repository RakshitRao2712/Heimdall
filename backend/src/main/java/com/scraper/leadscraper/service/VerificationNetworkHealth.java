package com.scraper.leadscraper.service;

import java.net.URI;
import java.util.*;

/** Only aggregate error counts and hosts survive parsing; URLs/tokens and response bodies are never retained. */
final class VerificationNetworkHealth {
    private final Map<String,String> hosts=new LinkedHashMap<>();
    private int networkFailures,rateLimits,serverFailures,denials,challengeTimeouts,challengeScriptErrors;
    void clear(){hosts.clear();networkFailures=rateLimits=serverFailures=denials=challengeTimeouts=challengeScriptErrors=0;}
    private boolean relevant(String host){return host!=null&&(host.equals("google.com")||host.endsWith(".google.com")||host.equals("gstatic.com")||host.endsWith(".gstatic.com")||host.equals("recaptcha.net")||host.endsWith(".recaptcha.net"));}
    private boolean verificationResource(URI uri){return relevant(uri.getHost())&&uri.getPath()!=null&&uri.getPath().contains("/recaptcha/");}
    void accept(String event) {
        try {
            var message=new tools.jackson.databind.ObjectMapper().readTree(event).path("message");var params=message.path("params");String id=params.path("requestId").asText("");
            switch(message.path("method").asText("")) {
                case "Network.requestWillBeSent" -> {URI uri=URI.create(params.path("request").path("url").asText());String host=uri.getHost();if(verificationResource(uri)){if(hosts.size()>=500)hosts.remove(hosts.keySet().iterator().next());hosts.put(id,host);}}
                case "Network.loadingFailed" -> {String error=params.path("errorText").asText("");if(hosts.remove(id)!=null&&!params.path("canceled").asBoolean(false)&&!error.equals("net::ERR_ABORTED"))networkFailures++;}
                case "Network.responseReceived" -> {URI uri=URI.create(params.path("response").path("url").asText());if(verificationResource(uri)){int status=params.path("response").path("status").asInt();if(status==429)rateLimits++;else if(status==403)denials++;else if(status>=500)serverFailures++;}}
                case "Network.loadingFinished" -> hosts.remove(id);
                default -> { }
            }
        }catch(Exception ignored){/* Malformed/irrelevant events do not expose raw provider data. */}
    }
    void resourceFailure(String url,int status,boolean failed){
        try{if(!verificationResource(URI.create(url)))return;if(failed)networkFailures++;else if(status==429)rateLimits++;else if(status==403)denials++;else if(status>=500)serverFailures++;}catch(Exception ignored){}
    }
    void acceptBrowserMessage(String message){
        if(message==null)return;
        if(message.contains("reCAPTCHA Timeout"))challengeTimeouts++;
        if(message.contains("solveSimpleChallenge is not defined"))challengeScriptErrors++;
    }
    String message(){
        if(challengeTimeouts>0)return "Google reCAPTCHA timed out inside the browser. Verification did not complete. Reload verification or try another discovery method.";
        if(challengeScriptErrors>0)return "Google’s verification page reported a JavaScript error. Verification may not complete; reload it or try another discovery method.";
        if(rateLimits>0)return "Google returned a rate limit for a required resource. Verification may not complete in this session.";
        if(denials>0)return "Google denied a required resource. Verification may not complete in this session.";
        if(networkFailures>0)return "Some Google verification requests failed to load. Check your network; you can reload verification once ready.";
        if(serverFailures>0)return "Google returned a server error for a required resource. Try verification again later.";
        return "No reCAPTCHA resource failures observed so far. Complete verification below; only Google can confirm success.";
    }
}
