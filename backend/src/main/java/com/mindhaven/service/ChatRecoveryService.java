package com.mindhaven.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.chat.recovery.enabled", havingValue = "true", matchIfMissing = true)
public class ChatRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(ChatRecoveryService.class);
    private final ChatTurnStore store;
    private final ChatService chat;
    public ChatRecoveryService(ChatTurnStore store, ChatService chat) { this.store = store; this.chat = chat; }

    @Scheduled(fixedDelayString = "${app.chat.recovery.delay-ms:30000}", initialDelayString = "${app.chat.recovery.delay-ms:30000}")
    public void recover() {
        try {
            for (var claim : store.pending()) {
                try { chat.recoverPrepared(claim); }
                catch (RuntimeException ignored) { log.warn("Chat checkpoint {} remains pending; recovery will retry", claim.id()); }
            }
        } catch (RuntimeException ignored) { log.warn("Chat checkpoint recovery temporarily unavailable"); }
    }
}
