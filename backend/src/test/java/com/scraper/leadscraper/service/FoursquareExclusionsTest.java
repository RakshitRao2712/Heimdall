package com.scraper.leadscraper.service;

import org.junit.jupiter.api.Test;
import java.net.http.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class FoursquareExclusionsTest {
    @Test @SuppressWarnings("unchecked") void skipsExistingBusinessesAndUsesRemainingCandidatesWithinOneRequest() throws Exception {
        var client=mock(HttpClient.class);var response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"results\":[{\"fsq_place_id\":\"1\",\"name\":\"Known\"},{\"fsq_place_id\":\"2\",\"name\":\"Fresh\"},{\"fsq_place_id\":\"3\",\"name\":\"Extra\"}]}");
        doReturn(response).when(client).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        var result=FoursquareDiscovery.searchInternal("cafes in Boston","key",1,()->false,p->{},lead->lead.name().equals("Known"),50,client);
        assertEquals(1,result.leads().size());assertEquals("Fresh",result.leads().getFirst().name());
        assertEquals(1,result.progress().found());assertEquals(1,result.progress().requests());
        var request=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client,times(1)).send(request.capture(),any(HttpResponse.BodyHandler.class));
        assertTrue(request.getValue().uri().getQuery().contains("limit=50"));
        var empty=FoursquareDiscovery.searchInternal("cafes in Boston","key",10,()->false,p->{},lead->true,50,client);
        assertTrue(empty.leads().isEmpty());assertEquals(0,empty.progress().found());
    }
}
