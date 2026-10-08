package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.config.GlpiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GlpiClient {
    private final GlpiConfig glpiConfig;
    private final RestClient restClient;

    public GlpiClient(GlpiConfig glpiConfig, RestClient restClient) {
        this.glpiConfig = glpiConfig;

        this.restClient = restClient;
    }

    private String initSession(){
        Map<String,Object> response = restClient.get()
                .uri("/initSession")
                .header("App-Token", glpiConfig.getAppToken())
                .header("Authorization", "user_token " + glpiConfig.getUserToken())
                .retrieve()
                .body(Map.class);

        return (String) response.get("session");
    }

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
}
