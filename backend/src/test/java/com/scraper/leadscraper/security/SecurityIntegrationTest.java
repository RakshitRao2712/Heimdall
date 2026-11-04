package com.scraper.leadscraper.security;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import com.scraper.leadscraper.auth.AccountStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=com.scraper.leadscraper.LeadScrapperApplication.class,properties={"spring.config.import=","GOOGLE_CLIENT_ID=","GOOGLE_CLIENT_SECRET=","RAZORPAY_KEY_ID=","RAZORPAY_KEY_SECRET="})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
 static Path dir;static{try{dir=Files.createTempDirectory("heimdall-security-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void stores(DynamicPropertyRegistry r){r.add("heimdall.auth.file",()->dir.resolve("accounts").toString());r.add("heimdall.history.directory",()->dir.resolve("history").toString());r.add("heimdall.files.directory",()->dir.resolve("files").toString());r.add("heimdall.profiles.directory",()->dir.resolve("profiles").toString());r.add("heimdall.teams.directory",()->dir.resolve("teams").toString());r.add("heimdall.billing.directory",()->dir.resolve("billing").toString());}
 @Autowired MockMvc mvc;@Autowired AccountStore accounts;@Autowired PasswordEncoder encoder;@Autowired ObjectMapper json;
 @Test void loginRotatesSessionAndCsrfProtectsSearchAndLogout()throws Exception{
  accounts.create("security@example.com",encoder.encode("Security-test-password!"));
  var csrf=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();var old=(MockHttpSession)csrf.getRequest().getSession();var token=json.readTree(csrf.getResponse().getContentAsString());old.setAttribute("com.scraper.leadscraper.auth.PlacesKeyController.key","test-only-key");
  var login=mvc.perform(post("/api/auth/login").session(old).header(token.path("headerName").asText(),token.path("token").asText()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"security@example.com\",\"password\":\"Security-test-password!\",\"rememberMe\":false}")).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)login.getRequest().getSession();assertNotEquals(old.getId(),session.getId());assertTrue(old.isInvalid());assertNull(session.getAttribute("com.scraper.leadscraper.auth.PlacesKeyController.key"));
  mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  for(String method:java.util.List.of("maps","foursquare"))mvc.perform(post("/api/workspace/search/"+method).session(session).param("query","salons")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());mvc.perform(post("/api/workspace/browser-verification/input").session(session).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/auth/logout").session(session).header(token.path("headerName").asText(),token.path("token").asText())).andExpect(status().isForbidden());
  var fresh=mvc.perform(get("/api/auth/csrf").session(session)).andReturn();var next=json.readTree(fresh.getResponse().getContentAsString());
  for(String method:java.util.List.of("maps","foursquare"))mvc.perform(post("/api/workspace/search/"+method).session(session).header(next.path("headerName").asText(),next.path("token").asText()).param("query","salons")).andExpect(status().isPaymentRequired());
  for (String provider : java.util.List.of("places", "foursquare")) {
   String path = "/api/workspace/" + provider + "-key";
   String credential = "synthetic-key-12345678901234567890";
   var keyRequest = post(path).session(session)
       .header(next.path("headerName").asText(), next.path("token").asText())
       .contentType(MediaType.APPLICATION_JSON);
   mvc.perform(keyRequest.content("{\"apiKey\":\"" + credential + "\"}"))
       .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
       .andExpect(content().json("{\"configured\":true}", JsonCompareMode.STRICT));
   mvc.perform(get(path).session(session)).andExpect(status().isOk())
       .andExpect(content().json("{\"configured\":true}", JsonCompareMode.STRICT));
   mvc.perform(keyRequest.content("{\"apiKey\":\"" + credential))
       .andExpect(status().isBadRequest())
       .andExpect(content().json("{\"detail\":\"Enter a valid API key.\"}", JsonCompareMode.STRICT));
  }
  mvc.perform(get("/api/workspace/browser-verification").session(session)).andExpect(status().isNotFound());
  mvc.perform(get("/api/workspace/search/maps").session(session).param("query","salons")).andExpect(status().isMethodNotAllowed());
  mvc.perform(post("/api/auth/logout").session(session).header(next.path("headerName").asText(),next.path("token").asText())).andExpect(status().isNoContent());assertTrue(session.isInvalid());
 }
 @Test void anonymousCannotReadPrivateData()throws Exception{mvc.perform(get("/api/workspace/history")).andExpect(status().isUnauthorized());mvc.perform(get("/api/workspace/browser-verification")).andExpect(status().isNotFound());mvc.perform(get("/api/workspace/browser-verification/frame").param("challengeId","unknown")).andExpect(status().isNotFound());mvc.perform(get("/api/workspace/teams")).andExpect(status().isUnauthorized());}
}
