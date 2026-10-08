package org.dahllab.opsservicedoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "glpi")
public class GlpiConfig {
    private String apiUrl;
    private String appToken;
    private String userToken;

    private String webUrl;

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

    @Bean
    public RestClient glpiRestClient() {
        return RestClient.builder()
                .baseUrl(getApiUrl())
                .build();
    }
}

