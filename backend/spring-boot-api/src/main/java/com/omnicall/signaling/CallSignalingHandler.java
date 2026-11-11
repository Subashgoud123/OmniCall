package com.omnicall.signaling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicall.security.JwtService;
import com.omnicall.security.TokenDenylist;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CallSignalingHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final TokenDenylist denylist;
    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public CallSignalingHandler(JwtService jwtService, TokenDenylist denylist, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.denylist = denylist;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String token = uri == null ? null : UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (token == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        try {
            JwtService.AccessClaims claims = jwtService.parse(token);
            if (denylist.isRevoked(claims.jti())) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }
            session.getAttributes().put("userId", claims.userId());
            sessions.computeIfAbsent(claims.userId(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
        } catch (RuntimeException exception) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof UUID id) {
            Set<WebSocketSession> open = sessions.get(id);
            if (open != null) {
                open.remove(session);
            }
        }
    }

    public void send(UUID userId, Map<String, Object> event) {
        Set<WebSocketSession> open = sessions.get(userId);
        if (open == null) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (IOException exception) {
            return;
        }
        for (WebSocketSession session : open) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException ignored) {
                open.remove(session);
            }
        }
    }
}
