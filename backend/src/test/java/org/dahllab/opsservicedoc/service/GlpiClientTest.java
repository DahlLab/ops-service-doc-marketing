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

// Kein @SpringBootTest hier - ich baue den RestClient selbst mit einem
// MockRestServiceServer, statt den ganzen Spring-Kontext zu starten.
// Das macht den Test deutlich schneller (KISS: nur so viel Infrastruktur
// wue nötig für das, was wir testen wollen).
class GlpiClientTest {

    private MockRestServiceServer mockServer;
    private GlpiClient glpiClient;

    @BeforeEach
    void setUp() {
        // Testkonfiguration mit Beispiel-Werten statt echter Zugangsdaten.
        GlpiConfig testConfig = new GlpiConfig();
        testConfig.setApiUrl("http://test-glpi/api.php/v1");
        testConfig.setAppToken("test-app-token");
        testConfig.setUserToken("test-user-token");

        RestClient.Builder builder = RestClient.builder().baseUrl(testConfig.getApiUrl());

        // MockRestServiceServer klinkt sich in den RestClient ein und lässt uns
        // festlegen, welche Antwort bei welcher erwarteten Anfrage zurückkommt,
        // OHNE dass wirklich ein Netzwerk-Request stattfindet.
        mockServer = MockRestServiceServer.bindTo(builder).build();

        glpiClient = new GlpiClient(testConfig, builder.build());

    }

    @Test
    @DisplayName("GIVEN gültige GLPI-Zugangsdaten WHEN getAllGlpiTickets aufgerufen wird THEN wird zuerst initSession und dann Tickets abgefragt")
    void getAllGlpiTickets_ruftInitSessionUndTicketsAb() {

        // GIVEN: Wir legen fest, was der (simulierte) GLPI-Server auf die beiden
        // erwarteten Requests antworten soll.

        // 1. Erwarteter Request: initSession mit den korrekten Headern
        mockServer.expect(requestTo("http://test-glpi/api.php/v1/initSession"))
                .andExpect(header("App-Token","test-app-token"))
                .andExpect(header("Authorization", "user_token test-user-token"))
                .andRespond(withSuccess("{\"session\":\"abc123\"}", MediaType.APPLICATION_JSON));

        // 2. Erwarteter Request: Ticket-Abruf MIT dem Session-Token aus Schritt 1
        mockServer.expect(requestTo("http://test-glpi/api.php/v1/Ticket"))
                .andExpect(header("Session-Token","abc123"))
                .andRespond(withSuccess("[{\"id\":1,\"name\":\"Testticket\"}]", MediaType.APPLICATION_JSON));

        // WHEN:
        List<Map<String, Object>> result = glpiClient.getAllGlpiTickets();

        // THEN:
        assertEquals(1, result.size());
        assertEquals("Testticket", result.get(0).get("name"));

        // Bestätigt, dass GENAU die erwarteten Requests (und keine anderen) gestellt wurden.
        mockServer.verify();

    }
}
