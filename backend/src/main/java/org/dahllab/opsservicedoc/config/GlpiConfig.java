package org.dahllab.opsservicedoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


// @ConfigurationProperties: bindet alle Properties mit dem mit dem Prefix "glpi."
// automatisch an die Felder dieser Klasse (glpi.api-url -> apiUrl, usw.).
// Vorteil gegenüber einzelnen @Value-Annotationen: typsicher, zentral an
// einer Stelle gebündelt, und IntelliJ bietet Autovervolsständigung dafür
// (DRY: eine Config-Quelle statt vieler verstreuter Value-Aufrufe).
@Configuration
@ConfigurationProperties(prefix = "glpi")
public class GlpiConfig {

    private String apiUrl;
    private String appToken;
    private String userToken;
    // Optional: Adresse der GLPI-Weboberfläche (für den Link im Frontend).
    private String webUrl;


    // Getter/Setter werden von Spring Boot für die automatische Befüllung
    // benötigt (kein Lombok hier, da @ConfigurationProperties klassische
    // Java-Bean-Konventionen erwartet.)
    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    // Adresse der GLPI-Weboberfläche: entweder explizit über glpi.web-url
    // gesetzt, sonst aus der API-URL abgeleitet (".../api.php/v1" bzw.
    // ".../apirest.php" abschneiden). Ich liefere nur echte http(s)-URLs
    // zurück: ist die Umgebungsvariable nicht gesetzt, bleibt bei
    // @ConfigurationProperties der Platzhalter "${GLPI_API_URL}" als
    // Text stehen - der würde im Frontend als relativer Link enden.
    // Dann gebe ich lieber "" zurück, und das Frontend blendet den Link aus.
    public String resolveWebUrl() {
        if (isRealUrl(webUrl)) {
            return webUrl;
        }
        if (!isRealUrl(apiUrl)) {
            return "";
        }
        int index = apiUrl.indexOf("/api.php");
        if (index < 0) {
            index = apiUrl.indexOf("/apirest.php");
        }
        return index > 0 ? apiUrl.substring(0, index) : apiUrl;
    }

    private static boolean isRealUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    // Stellt den fertig konfigurierten RestClient als Spring-Bean bereit.
    // Wird automatisch in GlpiClient injiziert (Constructor Injection).
    // Im Test wird stattdessen manuell ein RestClient mit
    // MockRestServiceServer gebaut, ohne dass diese Bean-Methode
    // überhaupt aufgerufen wird.
    @Bean
    public RestClient glpiRestClient() {
        return RestClient.builder()
                .baseUrl(getApiUrl())
                .build();
    }

}



