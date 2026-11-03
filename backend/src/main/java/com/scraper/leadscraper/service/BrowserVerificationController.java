package com.scraper.leadscraper.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Session-bound manual browser controls. WebDriver is only accessed by the search thread. */
@RestController
@RequestMapping("/api/workspace/browser-verification")
public class BrowserVerificationController {
    private static final ConcurrentHashMap<String,Pending> WAITING=new ConcurrentHashMap<>();
    static final Duration LIFETIME=Duration.ofMinutes(5);
    static final class Pending {
        final String id=UUID.randomUUID().toString();
        final Instant expiresAt;
        Pending(Duration lifetime){expiresAt=Instant.now().plus(lifetime);}
        final BlockingQueue<Input> inputs=new ArrayBlockingQueue<>(32);
        volatile byte[] frame;
        volatile String message="Complete Google verification in this view, then choose Resume search.";
        volatile boolean resume;
        volatile boolean focus;
        volatile boolean reload;
        volatile String diagnostic;
        java.time.Instant lastReload=java.time.Instant.EPOCH;
        boolean localWindow;
        final java.util.concurrent.atomic.AtomicLong appliedInputs=new java.util.concurrent.atomic.AtomicLong();
    }
    public record Input(String challengeId,String kind,Double x,Double y,String text){}
    public record Resume(String challengeId){}
    public record Status(boolean waiting,String challengeId,String expiresAt,String message,long appliedInputs,boolean localWindow,String diagnostic){}

    public static void await(HttpSession session,PlaywrightGoogleDiscovery browser,BooleanSupplier cancelled) {
        await(session,browser,cancelled,LIFETIME);
    }
    static void await(HttpSession session,PlaywrightGoogleDiscovery browser,BooleanSupplier cancelled,Duration lifetime) {
        String owner=session.getId();var pending=new Pending(lifetime);pending.localWindow=browser.visible();
        if(WAITING.putIfAbsent(owner,pending)!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"A browser verification is already open.");
        long nextDiagnostic=0;
        try {
            while(Instant.now().isBefore(pending.expiresAt)) {
                if(cancelled.getAsBoolean())throw new ResponseStatusException(HttpStatus.CONFLICT,"Browser search cancelled; verification closed.");
                if(pending.reload){pending.reload=false;browser.reloadVerification();}
                if(System.nanoTime()>=nextDiagnostic){pending.diagnostic=browser.verificationHealth();nextDiagnostic=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);}
                if(pending.focus){pending.focus=false;browser.focusVerification();}
                Input input;
                while((input=pending.inputs.poll())!=null){browser.manualInput(input);pending.appliedInputs.incrementAndGet();}
                if(pending.resume) {
                    if(browser.verificationReady())return;
                    pending.message="Waiting for Google to finish verification. Search will resume when results load.";
                }
                pending.frame=browser.verificationScreenshot();
                Thread.sleep(750);
            }
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT,"Google verification expired after five minutes. Start a new search to try again.");
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.CONFLICT,"Browser verification interrupted.");}
        finally{WAITING.remove(owner,pending);pending.frame=null;pending.inputs.clear();}
    }
    static Pending require(HttpSession session,String challengeId) {
        var pending=WAITING.get(session.getId());
        if(pending==null||!pending.id.equals(challengeId)||!Instant.now().isBefore(pending.expiresAt))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"This browser verification is no longer available.");
        return pending;
    }
    @GetMapping public Status status(HttpSession session) {
        var p=WAITING.get(session.getId());
        return p==null||!Instant.now().isBefore(p.expiresAt)?new Status(false,null,null,null,0,false,null):new Status(true,p.id,p.expiresAt.toString(),p.message,p.appliedInputs.get(),p.localWindow,p.diagnostic);
    }
    @GetMapping(value="/frame",produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> frame(@RequestParam String challengeId,HttpSession session) {
        var p=require(session,challengeId);byte[] frame=p.frame;
        if(frame==null)return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(frame);
    }
    @PostMapping("/input") public Map<String,Boolean> input(@RequestBody Input input,HttpSession session) {
        validate(input);var p=require(session,input.challengeId());
        if(!p.inputs.offer(input))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Wait for the browser to catch up before clicking again.");
        return Map.of("accepted",true);
    }
    static void validate(Input input) {
        if(input==null||input.challengeId()==null||input.kind()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid browser input.");
        boolean valid=switch(input.kind()) {
            case "click" -> input.x()!=null&&input.y()!=null&&Double.isFinite(input.x())&&Double.isFinite(input.y())&&input.x()>=0&&input.x()<1&&input.y()>=0&&input.y()<1;
            case "text" -> input.text()!=null&&!input.text().isEmpty()&&input.text().length()<=200&&input.text().chars().noneMatch(c->Character.isISOControl(c)||(c>=0xe000&&c<=0xf8ff));
            case "key" -> Set.of("TAB","ENTER","SPACE","BACKSPACE","ARROWUP","ARROWDOWN","ARROWLEFT","ARROWRIGHT").contains(input.text()==null?"":input.text());
            case "scroll" -> input.y()!=null&&Double.isFinite(input.y())&&Math.abs(input.y())<=600;
            default -> false;
        };
        if(!valid)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid browser input.");
    }
    @PostMapping("/resume") public Map<String,Boolean> resume(@RequestBody Resume input,HttpSession session) {
        if(input==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing verification.");
        require(session,input.challengeId()).resume=true;return Map.of("accepted",true);
    }
    @PostMapping("/focus") public Map<String,Boolean> focus(@RequestBody Resume input,HttpSession session) {
        if(input==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing verification.");
        var p=require(session,input.challengeId());if(!p.localWindow)throw new ResponseStatusException(HttpStatus.CONFLICT,"This browser is remote. Use the verification view instead.");
        p.focus=true;return Map.of("accepted",true);
    }
    @PostMapping("/reload") public Map<String,Boolean> reload(@RequestBody Resume input,HttpSession session) {
        if(input==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing verification.");
        var p=require(session,input.challengeId());synchronized(p){if(java.time.Duration.between(p.lastReload,Instant.now()).toSeconds()<30)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Wait 30 seconds before reloading verification again.");p.lastReload=Instant.now();p.reload=true;}
        return Map.of("accepted",true);
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail",Objects.requireNonNullElse(e.getReason(),"Browser verification unavailable.")));}
}
