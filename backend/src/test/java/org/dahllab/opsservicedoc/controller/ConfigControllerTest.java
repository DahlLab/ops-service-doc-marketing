package org.dahllab.opsservicedoc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // In der Test-Konfiguration ist glpi.api-url=http://localhost/api.php/v1
    // gesetzt, daraus wird die Web-Adresse http://localhost abgeleitet.
    @Test
    void getGlpiUrl_returnsDerivedWebUrl() throws Exception {
        mockMvc.perform(get("/api/config/glpi-url").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost"));
    }

    @Test
    void getGlpiUrl_returns401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/config/glpi-url"))
                .andExpect(status().isUnauthorized());
    }
}
