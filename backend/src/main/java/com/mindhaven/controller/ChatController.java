package com.mindhaven.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.ChatStreamRequest;
import com.mindhaven.dto.CreateSessionRequest;
import com.mindhaven.dto.EmotionAnalysisDto;
import com.mindhaven.dto.MessageDto;
import com.mindhaven.dto.SessionDto;
import com.mindhaven.dto.UpdateSessionRequest;
import com.mindhaven.entity.ChatMessage;
import com.mindhaven.entity.ChatSession;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final Executor chatExecutor;

    public ChatController(ChatService chatService, @Qualifier("chatExecutor") Executor chatExecutor) {
        this.chatService = chatService;
        this.chatExecutor = chatExecutor;
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionDto> createSession(@RequestBody CreateSessionRequest request) {
        return ApiResponse.success(SessionDto.of(chatService.createSession(AuthContextHolder.userId(), request)));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<SessionDto>> listSessions(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        var result = chatService.listSessions(AuthContextHolder.userId(), page, size);
        var items = result.getContent().stream().map(SessionDto::of).toList();
        return ApiResponse.success(PageResult.of(items, page, size, result.getTotalElements()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<SessionDto> getSession(@PathVariable Long sessionId) {
        return ApiResponse.success(SessionDto.of(chatService.getSession(AuthContextHolder.userId(), sessionId)));
    }

    @PatchMapping("/sessions/{sessionId}")
    public ApiResponse<SessionDto> updateSession(@PathVariable Long sessionId, @RequestBody UpdateSessionRequest request) {
        return ApiResponse.success(SessionDto.of(chatService.updateSession(AuthContextHolder.userId(), sessionId,
                request.title(), request.status())));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(AuthContextHolder.userId(), sessionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<PageResult<MessageDto>> listMessages(@PathVariable Long sessionId,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        var result = chatService.listMessages(AuthContextHolder.userId(), sessionId, page, size);
        var items = result.getContent().stream().map(MessageDto::of).toList();
        return ApiResponse.success(PageResult.of(items, page, size, result.getTotalElements()));
    }

    @GetMapping("/sessions/{sessionId}/emotion")
    public ApiResponse<EmotionAnalysisDto> emotion(@PathVariable Long sessionId) {
        return ApiResponse.success(chatService.emotionOf(AuthContextHolder.userId(), sessionId));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
        Long userId = AuthContextHolder.userId();
        SseEmitter emitter = new SseEmitter(chatService.sseTimeoutMs());
        emitter.onTimeout(emitter::complete);
        chatExecutor.execute(() -> chatService.stream(userId, request, emitter));
        return emitter;
    }
}
