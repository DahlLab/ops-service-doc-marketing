package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.config.GlpiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GlpiClientTest {
    private MockRestServiceServer mockServer;
    private GlpiClient glpiClient;

    @BeforeEach
    void setUp() {
        GlpiConfig testConfig = new GlpiConfig();
        testConfig.setApiUrl("http://test-glpi/api.php/v1");
        testConfig.setAppToken("test-app-token");
        testConfig.setUserToken("test-user-token");

        RestClient.Builder builder = RestClient.builder().baseUrl(testConfig.getApiUrl());

        mockServer = MockRestServiceServer.bindTo(builder).build();

        glpiClient = new GlpiClient(testConfig, builder.build());
    }

    @Test
    @DisplayName("GIVEN valid GLPI credentials WHEN getAllGlpiTickets is called THEN initSession is called first and the tickets are fetched afterwards")
    void getAllGlpiTickets_callsInitSessionAndFetchesTickets() {
        mockServer.expect(requestTo("http://test-glpi/api.php/v1/initSession"))
                .andExpect(header("App-Token","test-app-token"))
                .andExpect(header("Authorization", "user_token test-user-token"))
                .andRespond(withSuccess("{\"session\":\"abc123\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://test-glpi/api.php/v1/Ticket"))
                .andExpect(header("Session-Token","abc123"))
                .andRespond(withSuccess("[{\"id\":1,\"name\":\"Testticket\"}]", MediaType.APPLICATION_JSON));

        List<Map<String, Object>> result = glpiClient.getAllGlpiTickets();

        assertEquals(1, result.size());
        assertEquals("Testticket", result.get(0).get("name"));

        mockServer.verify();
    }
}
