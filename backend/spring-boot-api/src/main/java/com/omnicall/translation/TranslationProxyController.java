package com.omnicall.translation;

import com.omnicall.common.ApiException;
import com.omnicall.config.OmniCallProperties;
import com.omnicall.security.AuthContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranslationProxyController {

    private final OmniCallProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TranslationProxyController(OmniCallProperties properties) {
        this.properties = properties;
    }

    @PostMapping("/api/translation/text")
    public ResponseEntity<String> text(@RequestBody String body) {
        AuthContext.current();
        return forward(HttpRequest.newBuilder(URI.create(properties.getAi().getBaseUrl() + "/api/translation/text"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    @PostMapping(value = "/api/translation/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> speech(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLanguage") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage) throws IOException {
        AuthContext.current();
        String boundary = "omnicall-" + java.util.UUID.randomUUID();
        byte[] payload = multipart(boundary, file, sourceLanguage, targetLanguage);
        return forward(HttpRequest.newBuilder(URI.create(properties.getAi().getBaseUrl() + "/api/translation/speech"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build());
    }

    private ResponseEntity<String> forward(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(response.statusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.body());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_UNAVAILABLE", "The translation service is unavailable.");
        }
    }

    private byte[] multipart(String boundary, MultipartFile file, String sourceLanguage, String targetLanguage) throws IOException {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"sourceLanguage\"\r\n\r\n" + sourceLanguage + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"targetLanguage\"\r\n\r\n" + targetLanguage + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String ending = "\r\n--" + boundary + "--\r\n";
        byte[] head = header.getBytes(StandardCharsets.UTF_8);
        byte[] tail = ending.getBytes(StandardCharsets.UTF_8);
        byte[] audio = file.getBytes();
        byte[] all = new byte[head.length + audio.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(audio, 0, all, head.length, audio.length);
        System.arraycopy(tail, 0, all, head.length + audio.length, tail.length);
        return all;
    }
}
