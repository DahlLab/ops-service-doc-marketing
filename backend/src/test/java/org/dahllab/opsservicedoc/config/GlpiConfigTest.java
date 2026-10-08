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
    @DisplayName("GIVEN glpi.* properties are set WHEN the application starts THEN they are bound correctly in GlpiConfig")
    void glpiProperties_areBoundCorrectly() {

        // GIVEN: die Properties sind über @TestPropertySource oben bereits gesetzt.

        // WHEN: Spring Boot hat GlpiConfi beim Start automatisch befüllt
        // (das passiert implizit durch @EnableConfigurationProperties).

        // THEN: die Werte in der Bean müssen exakt den gesetzten Poperties entsprechen.
        assertEquals("http://test-glpi/api.php/v1",  glpiConfig.getApiUrl());
        assertEquals("test-app-token", glpiConfig.getAppToken());
        assertEquals("test-user-token", glpiConfig.getUserToken());

    }

    @Test
    @DisplayName("GIVEN no glpi.web-url WHEN resolveWebUrl is called THEN it is derived from the API URL")
    void resolveWebUrl_derivesFromApiUrl() {
        assertEquals("http://test-glpi", glpiConfig.resolveWebUrl());
    }

    @Test
    @DisplayName("GIVEN an explicit web-url WHEN resolveWebUrl is called THEN it takes precedence")
    void resolveWebUrl_prefersExplicitWebUrl() {
        GlpiConfig config = new GlpiConfig();
        config.setApiUrl("http://x/api.php/v1");
        config.setWebUrl("http://glpi.example");
        assertEquals("http://glpi.example", config.resolveWebUrl());
    }

    @Test
    @DisplayName("GIVEN no URLs WHEN resolveWebUrl is called THEN an empty string is returned; apirest.php and plain URLs are supported")
    void resolveWebUrl_edgeCases() {
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
