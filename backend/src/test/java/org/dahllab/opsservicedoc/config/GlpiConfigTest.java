package org.dahllab.opsservicedoc.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

// @SpringBootTest lädt hier bewusst NICHT die ganze Anwendung, sondern nur
// das Nötigste, siehe @EnableConfigurationProperties unten, das gezielt
// nur GlpiConfig registriert, statt den kompletten Anwendungskotext zu starten
// (KISS: minimaler Testkontext für eine reine Konfigurationsklasse).
@SpringBootTest(classes = GlpiConfig.class)
@EnableConfigurationProperties(GlpiConfig.class)
//
//
//
@TestPropertySource(properties = {
        "glpi.api-url=http://test-glpi/api.php/v1",
        "glpi.app-token=test-app-token",
        "glpi.user-token=test-user-token"
})
class GlpiConfigTest {

    @Autowired
    private GlpiConfig glpiConfig;

    @Test
    @DisplayName("GIVEN gestzte glpi.*-Properties WHEN die Anwendung startet THEN werden sie korrekt in GlpiConfig gebunden")
    void glpiProperties_werdenKorrektGebunden() {

        // GIVEN: die Properties sind über @TestPropertySource oben bereits gesetzt.

        // WHEN: Spring Boot hat GlpiConfi beim Start automatisch befüllt
        // (das passiert implizit durch @EnableConfigurationProperties).

        // THEN: die Werte in der Bean müssen exakt den gesetzten Poperties entsprechen.
        assertEquals("http://test-glpi/api.php/v1",  glpiConfig.getApiUrl());
        assertEquals("test-app-token", glpiConfig.getAppToken());
        assertEquals("test-user-token", glpiConfig.getUserToken());

    }

    @Test
    @DisplayName("GIVEN keine glpi.web-url WHEN resolveWebUrl THEN wird sie aus der API-URL abgeleitet")
    void resolveWebUrl_leitetAusApiUrlAb() {
        assertEquals("http://test-glpi", glpiConfig.resolveWebUrl());
    }

    @Test
    @DisplayName("GIVEN explizite web-url WHEN resolveWebUrl THEN hat sie Vorrang")
    void resolveWebUrl_bevorzugtExpliziteWebUrl() {
        GlpiConfig config = new GlpiConfig();
        config.setApiUrl("http://x/api.php/v1");
        config.setWebUrl("http://glpi.example");
        assertEquals("http://glpi.example", config.resolveWebUrl());
    }

    @Test
    @DisplayName("GIVEN keine URLs WHEN resolveWebUrl THEN leerer String; apirest.php und reine URL werden unterstützt")
    void resolveWebUrl_randfaelle() {
        GlpiConfig config = new GlpiConfig();
        assertEquals("", config.resolveWebUrl());
        config.setApiUrl("http://h/glpi/apirest.php");
        assertEquals("http://h/glpi", config.resolveWebUrl());
        config.setApiUrl("http://h/plain");
        assertEquals("http://h/plain", config.resolveWebUrl());
        // Nicht aufgelöster Platzhalter (Umgebungsvariable fehlt) -> kein Link.
        config.setApiUrl("${GLPI_API_URL}");
        assertEquals("", config.resolveWebUrl());
        config.setWebUrl("${GLPI_WEB_URL}");
        assertEquals("", config.resolveWebUrl());
    }
}
