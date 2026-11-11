package com.omnicall.call;

import com.omnicall.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallController {

    private final CallService calls;
    private final com.omnicall.config.OmniCallProperties properties;

    public CallController(CallService calls, com.omnicall.config.OmniCallProperties properties) {
        this.calls = calls;
        this.properties = properties;
    }

    @PostMapping("/api/calls")
    public CallService.CallView start(@Valid @RequestBody CallService.StartCallRequest request) {
        return calls.start(AuthContext.current().id(), request.receiverId());
    }

    @PostMapping("/api/calls/{callId}/accept")
    public CallService.CallView accept(@PathVariable UUID callId) {
        return calls.accept(AuthContext.current().id(), callId);
    }

    @PostMapping("/api/calls/{callId}/reject")
    public CallService.CallView reject(@PathVariable UUID callId) {
        return calls.reject(AuthContext.current().id(), callId);
    }

    @PostMapping("/api/calls/{callId}/end")
    public CallService.CallView end(@PathVariable UUID callId) {
        return calls.end(AuthContext.current().id(), callId);
    }

    @GetMapping("/api/calls/history")
    public List<CallService.CallView> history() {
        return calls.history(AuthContext.current().id());
    }

    @GetMapping("/api/calls/{callId}")
    public CallService.CallView get(@PathVariable UUID callId) {
        return calls.get(AuthContext.current().id(), callId);
    }

    @GetMapping("/api/calls/{callId}/token")
    public CallService.CallToken token(@PathVariable UUID callId) {
        return calls.token(AuthContext.current().id(), callId);
    }

    @PostMapping("/api/internal/calls/{callId}/translations")
    public void translation(
            @RequestHeader("X-OmniCall-Internal-Token") String token,
            @PathVariable UUID callId,
            @RequestBody CallService.TranslationReport report) {
        if (!properties.getInternalToken().equals(token)) {
            throw new com.omnicall.common.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INTERNAL_UNAUTHORIZED",
                    "The translator is not authorized.");
        }
        calls.recordTranslation(callId, report);
    }
}
