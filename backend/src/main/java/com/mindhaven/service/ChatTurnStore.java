package com.mindhaven.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.dto.ChatStreamRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Checkpoint and business writes use the same datasource and transaction manager. */
@Service
public class ChatTurnStore {
    public record Claim(long id, long sessionId, long userId, String owner, PreparedChatTurn prepared) {}
    public record Delivery(long messageId, PreparedChatTurn prepared) {}
    private record Row(long id, String hash, String status, String owner, LocalDateTime lease, String result, Long messageId) {}
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper mapper;

    public ChatTurnStore(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.mapper = mapper;
    }

    public Claim claim(long userId, ChatStreamRequest request) {
        return transactions.execute(status -> {
            lockSession(userId, request.sessionId());
            String hash = digest(request.message().trim());
            var active = jdbc.queryForList("select id from mh_chat_turn where session_id=? and client_request_id<>? and "
                    + "((status='GENERATING' and lease_until>?) or status='PREPARED')", Long.class,
                    request.sessionId(), request.requestId(), Timestamp.valueOf(LocalDateTime.now()));
            if (!active.isEmpty()) throw conflict("上一轮对话仍在处理或保存中，请稍后刷新重试");
            var rows = jdbc.query("select * from mh_chat_turn where session_id=? and client_request_id=?", this::row,
                    request.sessionId(), request.requestId());
            String owner = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            if (!rows.isEmpty()) {
                Row row = rows.getFirst();
                if (!hash.equals(row.hash())) throw conflict("同一请求标识不能用于不同消息");
                if (!"GENERATING".equals(row.status())) {
                    return new Claim(row.id(), request.sessionId(), userId, null, decode(row.result()));
                }
                if (row.lease() != null && row.lease().isAfter(now)) throw conflict("该请求正在处理中，请稍后使用同一请求标识重试");
                jdbc.update("update mh_chat_turn set owner_token=?, lease_until=? where id=?", owner, Timestamp.valueOf(now.plusMinutes(5)), row.id());
                return new Claim(row.id(), request.sessionId(), userId, owner, null);
            }
            jdbc.update("insert into mh_chat_turn (user_id,session_id,client_request_id,message_hash,status,owner_token,lease_until) values (?,?,?,?,'GENERATING',?,?)",
                    userId, request.sessionId(), request.requestId(), hash, owner, Timestamp.valueOf(now.plusMinutes(5)));
            Long id = jdbc.queryForObject("select id from mh_chat_turn where session_id=? and client_request_id=?", Long.class, request.sessionId(), request.requestId());
            return new Claim(id, request.sessionId(), userId, owner, null);
        });
    }

    public void checkpoint(Claim claim, PreparedChatTurn prepared) {
        String snapshot;
        try { snapshot = mapper.writeValueAsString(prepared); }
        catch (java.io.IOException e) { throw new BizException(ErrorCode.INTERNAL_ERROR, "无法保存已审核结果"); }
        transactions.executeWithoutResult(status -> {
            lockSession(claim.userId(), claim.sessionId());
            int changed = jdbc.update("update mh_chat_turn set status='PREPARED', result_json=?, owner_token=null, lease_until=null "
                            + "where id=? and status='GENERATING' and owner_token=?", snapshot, claim.id(), claim.owner());
            if (changed != 1) throw conflict("请求处理权已变更，请使用同一请求标识重试");
        });
    }

    public Delivery finish(Claim claim, Function<PreparedChatTurn, Long> persist) {
        return transactions.execute(status -> {
            lockSession(claim.userId(), claim.sessionId());
            Row row = jdbc.query("select * from mh_chat_turn where id=? for update", this::row, claim.id()).getFirst();
            PreparedChatTurn prepared = decode(row.result());
            if ("COMPLETED".equals(row.status())) return new Delivery(row.messageId(), prepared);
            if (!"PREPARED".equals(row.status())) throw conflict("结果尚未准备完成，请稍后重试");
            // All repository writes in this callback join this transaction. No SSE is sent here.
            long messageId = persist.apply(prepared);
            jdbc.update("update mh_chat_turn set status='COMPLETED', assistant_message_id=? where id=?", messageId, claim.id());
            return new Delivery(messageId, prepared);
        });
    }

    public void release(Claim claim) {
        if (claim == null || claim.owner() == null) return;
        jdbc.update("update mh_chat_turn set lease_until=? where id=? and status='GENERATING' and owner_token=?",
                Timestamp.valueOf(LocalDateTime.now()), claim.id(), claim.owner());
    }

    public void deleteSession(long sessionId) {
        jdbc.update("delete from mh_chat_turn where session_id=?", sessionId);
    }

    public java.util.List<Claim> pending() {
        return jdbc.query("select t.* from mh_chat_turn t join mh_chat_session s on s.id=t.session_id "
                        + "where t.status='PREPARED' and s.deleted_at is null and s.status='ACTIVE' order by t.id limit 20",
                (rs, index) -> new Claim(rs.getLong("id"), rs.getLong("session_id"), rs.getLong("user_id"), null, decode(rs.getString("result_json"))));
    }

    private void lockSession(long userId, long sessionId) {
        var sessions = jdbc.queryForList("select id from mh_chat_session where id=? and user_id=? and deleted_at is null and status='ACTIVE' for update", Long.class, sessionId, userId);
        if (sessions.isEmpty()) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "会话不存在或已关闭");
    }

    private Row row(java.sql.ResultSet rs, int index) throws java.sql.SQLException {
        Timestamp lease = rs.getTimestamp("lease_until");
        Number messageId = (Number) rs.getObject("assistant_message_id");
        return new Row(rs.getLong("id"), rs.getString("message_hash"), rs.getString("status"), rs.getString("owner_token"),
                lease == null ? null : lease.toLocalDateTime(), rs.getString("result_json"), messageId == null ? null : messageId.longValue());
    }

    private PreparedChatTurn decode(String snapshot) {
        try {
            if (snapshot == null) throw new java.io.IOException();
            return mapper.readValue(snapshot, PreparedChatTurn.class);
        } catch (java.io.IOException e) { throw new BizException(ErrorCode.INTERNAL_ERROR, "已保存结果不可用，请联系维护者"); }
    }

    private String digest(String message) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(message.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private BizException conflict(String message) { return new BizException(ErrorCode.RESOURCE_CONFLICT, message); }
}
