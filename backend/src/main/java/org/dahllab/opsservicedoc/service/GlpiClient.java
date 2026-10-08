package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.config.GlpiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// GlpiClient: kapselt die komplette Kommunikation mit der GLPI-REST-API.
// Andere Klassen (z.B. TicketService) müssen sich NICHT mit GLPI-spezifischen
// Details (Session-Handling, Header-Namen) beschäftigen, sie rufen nur
// die Methoden dieser Klasse auf (Single Responsibility: GLPI-Kommunikation
// ist hier zentral gebündelt, nicht über mehrere Klassen verstreut).
@Service
public class GlpiClient {

    private final GlpiConfig glpiConfig;
    private final RestClient restClient;

    public GlpiClient(GlpiConfig glpiConfig, RestClient restClient) {
        this.glpiConfig = glpiConfig;
        // RestClient: Spring Boot moderner HTTP-Client (Nachfolger von RestTemplate),
        // hier fest auf die GLPI-Basis-URL konfiguriert, damit wir bei jedem
        // Aufruf nicht die volle URL wiederholen müssen (DRY).
        this.restClient = restClient;
    }

    // Schritt 1 des GLPI-Auth-Flows: fragt einen Session-Token an.
    // Dieser wird für JEDEN weiteren API-Aufruf  benötigt.
    // GLPI erwartet dafür App-Token und User-Token als spezielle HTTP-Header
    // (nicht als klassischer Authorization-Header mit Bearer-Token).
    private String initSession(){
        Map<String,Object> response = restClient.get()
                .uri("/initSession")
                .header("App-Token", glpiConfig.getAppToken())
                .header("Authorization", "user_token " + glpiConfig.getUserToken())
                .retrieve()
                .body(Map.class);

        return (String) response.get("session");

    }

    // Ruft alle Tickets von GLPI ab. Holt sich zuerst einen frischen
    // Session-Token (siehe initSession()), dann den eigentlichen Ticket-Abruf.
    //
    // Rückgabetyp bewusst List<Map<String, Object>> statt eines eigenen
    // GLPI-DTOs: GLPI liefert ein generisches JSON-Array zurück, dessen
    // Feldnamen ich erst im GlpiTicketMapper gezielt auf mein eigenes
    // Ticket-Model abbilden (KISS: keine zusätzliche Zwischenklasse nur
    // für die rohen GLPI-Rückgabedaten).
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> getAllGlpiTickets() {
        String session = initSession();

        return restClient.get()
                .uri("/Ticket")
                .header("App-Token", glpiConfig.getAppToken())
                .header("Session-Token", session)
                .retrieve()
                .body(List.class);
    }

    // @SuppressWarnings("unchecked") unterdrückt eine Compiler-Warnung, die entsteht,
    // weil body(List.class) technisch nur ein "rohes" List zurückgibt,
    // ohne dass Java zur Compile-Zeit weiß, dass es wirklich
    // List<Map<String, Object>> ist (sogenannte "unchecked cast").
    // Das ist hier bewusst in Kauf genommen, da GLPI uns nur generisches JSON liefert.



}
