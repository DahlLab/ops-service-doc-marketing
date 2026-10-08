package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dahllab.opsservicedoc.config.GlpiConfig;
import org.dahllab.opsservicedoc.dto.GlpiUrlDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Configuration", description = "Configuration values for the frontend")
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final GlpiConfig glpiConfig;

    public ConfigController(GlpiConfig glpiConfig) {
        this.glpiConfig = glpiConfig;
    }

    @Operation(summary = "Get the address of the GLPI web interface")
    @ApiResponse(responseCode = "200", description = "URL (empty if not configured)")
    @GetMapping("/glpi-url")
    public GlpiUrlDto getGlpiUrl() {
        return new GlpiUrlDto(glpiConfig.resolveWebUrl());
    }
}
