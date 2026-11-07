package com.scraper.leadscraper.billing;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class RazorpayWebhookController {
 private final Billing billing;
 public RazorpayWebhookController(Billing billing){this.billing=billing;}
 @PostMapping("/api/billing/razorpay/webhook")
 public Map<String,Boolean> receive(@RequestBody String body,@RequestHeader(value="X-Razorpay-Signature",required=false) String signature)throws Exception{billing.webhook(body,signature);return Map.of("received",true);}
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
 public org.springframework.http.ResponseEntity<?> error(org.springframework.web.server.ResponseStatusException e){return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(Map.of("detail",e.getReason()));}
}
