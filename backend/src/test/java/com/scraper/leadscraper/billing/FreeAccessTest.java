package com.scraper.leadscraper.billing;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class FreeAccessTest {
 @Test void signedInAccountCanSearchWithoutPurchase() throws Exception {
  var request = new MockHttpServletRequest();
  request.setUserPrincipal(() -> "unpaid@example.com");
  assertTrue(PremiumAccess.authenticatedAccess().preHandle(request, new MockHttpServletResponse(), new Object()));
 }
 @Test void anonymousSearchStillRequiresAuthentication() throws Exception {
  var response = new MockHttpServletResponse();
  assertFalse(PremiumAccess.authenticatedAccess().preHandle(new MockHttpServletRequest(), response, new Object()));
  assertEquals(401, response.getStatus());
 }
 @Test void checkoutCannotCreateNewCharges() {
  var controller = new BillingController(null);
  var error = assertThrows(ResponseStatusException.class, () -> controller.order(() -> "user@example.com"));
  assertEquals(410, error.getStatusCode().value());
 }
}
