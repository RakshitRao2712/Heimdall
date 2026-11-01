package com.scraper.leadscraper.billing;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
@RestController
@RequestMapping("/api/workspace/billing")
public class BillingController {
 private final Billing billing; public BillingController(Billing billing){this.billing=billing;}
 public record Payment(String razorpay_order_id,String razorpay_payment_id,String razorpay_signature){}
 @GetMapping public Map<String,Object> status(Principal user){return Map.of("premium",billing.premium(user.getName()),"configured",billing.configured(),"testMode",billing.testMode());}
 @PostMapping("/order") public Map<String,Object> order(Principal user)throws Exception{return billing.order(user.getName());}
 @PostMapping("/verify") public Map<String,Object> verify(@RequestBody Payment p,Principal user)throws Exception{billing.verify(user.getName(),p.razorpay_order_id(),p.razorpay_payment_id(),p.razorpay_signature());return status(user);}
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public org.springframework.http.ResponseEntity<?> error(org.springframework.web.server.ResponseStatusException e){return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(Map.of("detail",e.getReason()));}
}
