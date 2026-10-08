package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dahllab.opsservicedoc.config.GlpiConfig;
import org.dahllab.opsservicedoc.dto.GlpiUrlDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Stellt dem Frontend Konfigurationswerte bereit, die nicht fest im
// Frontend stehen sollen (hier: Adresse der GLPI-Weboberfläche für den
// Navigations-/Startseiten-Link). Tokens werden hier NIE ausgegeben.
@Tag(name = "Konfiguration", description = "Konfigurationswerte für das Frontend")
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final GlpiConfig glpiConfig;

    public ConfigController(GlpiConfig glpiConfig) {
        this.glpiConfig = glpiConfig;
    }

    @Operation(summary = "Adresse der GLPI-Weboberfläche abrufen")
    @ApiResponse(responseCode = "200", description = "URL (leer, falls nicht konfiguriert)")
    @GetMapping("/glpi-url")
    public GlpiUrlDto getGlpiUrl() {
        return new GlpiUrlDto(glpiConfig.resolveWebUrl());
    }
}
