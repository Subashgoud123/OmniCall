package com.omnicall.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.omnicall.config.OmniCallProperties;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);
    private final OmniCallProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PushSender(OmniCallProperties properties) {
        this.properties = properties;
    }

    public boolean configured() {
        return properties.getFcm().getCredentialsPath() != null
                && !properties.getFcm().getCredentialsPath().isBlank()
                && properties.getFcm().getProjectId() != null
                && !properties.getFcm().getProjectId().isBlank();
    }

    public void sendIncomingCall(String deviceToken, String title, String body, Map<String, String> data) {
        if (!configured()) {
            return;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(properties.getFcm().getCredentialsPath()))
                    .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
            credentials.refreshIfExpired();
            String access = credentials.getAccessToken().getTokenValue();
            String payload = """
                    {"message":{"token":"%s","notification":{"title":"%s","body":"%s"},"data":%s,"android":{"priority":"HIGH","notification":{"channel_id":"omnicall_incoming"}}}}
                    """.formatted(escape(deviceToken), escape(title), escape(body), dataJson(data));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://fcm.googleapis.com/v1/projects/" + properties.getFcm().getProjectId() + "/messages:send"))
                    .header("Authorization", "Bearer " + access)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("FCM returned {}", response.statusCode());
            }
        } catch (Exception exception) {
            log.warn("FCM send failed: {}", exception.getMessage());
        }
    }

    private String dataJson(Map<String, String> data) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (var entry : data.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escape(entry.getKey())).append("\":\"").append(escape(entry.getValue())).append('"');
        }
        return builder.append('}').toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
