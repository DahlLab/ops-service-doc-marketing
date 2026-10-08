package org.dahllab.opsservicedoc.dto;

// Antwort von GET /api/config/glpi-url: Adresse der GLPI-Weboberfläche.
public record GlpiUrlDto(String url) {
}
