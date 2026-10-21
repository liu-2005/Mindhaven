package com.mindhaven.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.config.AppProperties;
import com.mindhaven.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opaque bearer credentials; only SHA-256 digests are stored in the existing session table. */
@Service
public class LoginSessionService {
    private final JdbcTemplate jdbc;
    private final AppProperties properties;
    private final UserService users;
    private final SecureRandom random = new SecureRandom();

    public LoginSessionService(JdbcTemplate jdbc, AppProperties properties, UserService users) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.users = users;
    }

    public String create(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("insert into mh_auth_session (user_id, token_hash, expires_at) values (?, ?, ?)",
                userId, digest(token), Timestamp.valueOf(LocalDateTime.now().plusSeconds(properties.getJwtTtlSeconds())));
        return token;
    }

    public AuthContext authenticate(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        var identities = jdbc.query("select s.user_id, u.username from mh_auth_session s join mh_user u on u.id=s.user_id "
                        + "where s.token_hash=? and s.revoked_at is null and s.expires_at>? and u.deleted_at is null and u.status='ACTIVE'",
                (row, index) -> new AuthContext(row.getLong("user_id"), row.getString("username"), java.util.List.of()),
                digest(token), Timestamp.valueOf(LocalDateTime.now()));
        if (identities.isEmpty()) return null;
        AuthContext identity = identities.getFirst();
        return new AuthContext(identity.userId(), identity.username(), users.rolesOf(identity.userId()));
    }

    public void revoke(String token) {
        if (token != null) jdbc.update("update mh_auth_session set revoked_at=?, revoke_reason='logout' where token_hash=? and revoked_at is null",
                Timestamp.valueOf(LocalDateTime.now()), digest(token));
    }

    @Transactional
    public String rotate(String token, Long userId) {
        int changed = jdbc.update("update mh_auth_session set revoked_at=?, revoke_reason='rotated' "
                        + "where token_hash=? and user_id=? and revoked_at is null and expires_at>?",
                Timestamp.valueOf(LocalDateTime.now()), digest(token), userId, Timestamp.valueOf(LocalDateTime.now()));
        if (changed != 1) throw new BizException(ErrorCode.UNAUTHENTICATED);
        return create(userId);
    }

    private String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
