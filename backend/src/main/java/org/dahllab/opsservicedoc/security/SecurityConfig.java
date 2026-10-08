package org.dahllab.opsservicedoc.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

// @Configuration: sagt Spring, dass diese Klasse Bean-Definitionen enthält,
// die beim Hochfahren der App geladen werden sollen
@Configuration
// @EnableWebSecurity: aktiviert Spring Security für die gesamte Web-Anwendung
@EnableWebSecurity
public class SecurityConfig {

    // URL meines Frontends, konfigurierbar über die Umgebungsvariable
    // FRONTEND_URL (siehe application.properties), mit localhost:5173
    // als Fallback für die lokale Entwicklung. So bleibt der Code
    // unverändert, wenn ich später auf einem echten Server hoste -
    // ich muss dann nur die Umgebungsvariable setzen, statt hier im
    // Code eine feste URL zu hinterlegen (Review-Hinweis von Mahir).
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // @Bean: diese Methode liefert ein Objekt, das Spring verwaltet (hier die zentrale Security-Konfiguration)
    // SecurityFilterChain: die Kette von Filtern, die JEDE eingehende HTTP-Anfrage durchläuft,
    // bevor sie beim eigentlichen Controller ankommt
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // CSRF-Schutz (Cross-Site Request Forgery) ist AKTIV. Meine API
                // authentifiziert über das Session-Cookie, und der Browser schickt
                // Cookies bei jeder Anfrage an meine Domain automatisch mit - auch
                // wenn die Anfrage von einer fremden Seite ausgelöst wird. Ohne
                // CSRF-Token könnte so eine fremde Seite im Namen eines eingeloggten
                // Nutzers z.B. Tickets löschen. Ein Token, das nur meine eigene
                // Seite kennt, verhindert das.
                // Ablauf: Das Backend legt das Token als Cookie "XSRF-TOKEN" ab
                // (nicht HttpOnly, damit mein React-Code es lesen kann), das Frontend
                // schickt es bei schreibenden Aufrufen (POST/PUT/DELETE) im Header
                // "X-XSRF-TOKEN" zurück (siehe api.ts). Eine fremde Seite kann mein
                // Cookie nicht lesen und deshalb den Header nicht setzen.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // NOSONAR: CSRF-Cookie muss fuer die SPA lesbar sein (Double-Submit), die Session-Cookie bleibt HttpOnly
                        .csrfTokenRequestHandler(csrfTokenHandler()))

                // Regeln, WELCHE Endpoints WELCHEN Zugriffsschutz brauchen.
                // WICHTIG: Die Reihenfolge zählt! Spring prüft von oben nach unten
                // und nimmt die erste passende Regel - spezifische Regeln müssen VOR allgemeinen stehen
                .authorizeHttpRequests(req -> req
                        // ALLE Endpoints unter /api/ nur für eingeloggte Nutzer.
                        // Vorher hatte ich jeden Controller einzeln aufgelistet
                        // (tickets, tasks, ipd, auth/me) - dabei habe ich die
                        // Checklisten und Checklisten-Vorlagen vergessen, die
                        // dadurch ohne Login erreichbar waren (anyRequest().permitAll()
                        // unten). Mit dem Muster /api/** ist ein neuer Controller
                        // automatisch geschützt, und ich muss hier nichts mehr
                        // nachtragen. Login-Pfade (/oauth2/**, /login/**) liegen
                        // außerhalb von /api/ und bleiben dadurch zugänglich.
                        .requestMatchers("/api/**").authenticated()
                        // Alles andere (z.B. die Login-Weiterleitung selbst) für jeden zugänglich
                        .anyRequest().permitAll()
                )

                // Was passiert, wenn ein NICHT eingeloggter User auf eine geschützte Route zugreift?
                // Standardmäßig würde Spring Security versuchen, auf eine Login-Seite umzuleiten (HTML-Verhalten).
                // Da ich eine REST-API ist, will ich stattdessen einfach einen 401-Statuscode zurückgeben,
                // den das Frontend dann selbst auswerten kann (z.B. um zur Login-Seite zu routen)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        ))

                // Aktiviert den OAuth2-Login-Flow (GitHub, wie in application.properties konfiguriert).
                // defaultSuccessUrl: wohin der Browser nach erfolgreichem Login weitergeleitet wird -
                // hier zurück zu meinem laufenden React-Frontend
                .oauth2Login(o -> o.defaultSuccessUrl(frontendUrl + "/", true))

                // Wohin nach dem Logout weitergeleitet wird - ebenfalls zurück zum Frontend
                .logout(logout -> logout.logoutSuccessUrl(frontendUrl + "/"));

        // Baut die konfigurierte Filterkette und gibt sie an Spring zurück
        return http.build();
    }

    // Handler für das CSRF-Token. Zwei Details für meine SPA:
    // 1. CsrfTokenRequestAttributeHandler statt des Standard-Handlers: der
    //    Standard verschlüsselt das Token pro Antwort (BREACH-Schutz), dann
    //    stimmt der Cookie-Wert nicht mit dem Header-Wert überein, den
    //    React aus dem Cookie liest.
    // 2. setCsrfRequestAttributeName(null) schaltet das "verzögerte" Laden
    //    ab: das Cookie wird so bei JEDER Antwort gesetzt (auch bei
    //    GET /api/auth/me beim Seitenstart), nicht erst wenn jemand das
    //    Token anfordert - sonst hätte React beim ersten POST noch keins.
    private static CsrfTokenRequestAttributeHandler csrfTokenHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }
}
