package com.mindhaven.security;

import java.util.Arrays;
import java.util.List;

/**
 * 轻量 HMAC-SHA256 签名令牌（开发/本地环境使用，不依赖外部库）。
 * 生产环境应改用成熟 JWT 库并引入撤销/刷新机制。
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    public static String create(String secret, long userId, String username, List<String> roles, long ttlMillis) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = System.currentTimeMillis() + ttlMillis;
        String payload = base64Url("{\"sub\":%d,\"name\":\"%s\",\"roles\":%s,\"exp\":%d}"
                .formatted(userId, escape(username), toJsonArray(roles), exp));
        String signingInput = header + "." + payload;
        String signature = base64Url(sign(signingInput, secret));
        return signingInput + "." + signature;
    }

    public static AuthContext parse(String secret, String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = base64Url(sign(signingInput, secret));
        if (!constantTimeEquals(expected, parts[2])) {
            return null;
        }
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        long exp = extractLong(payloadJson, "\"exp\"");
        if (exp > 0 && exp < System.currentTimeMillis()) {
            return null;
        }
        long userId = extractLong(payloadJson, "\"sub\"");
        String name = extractString(payloadJson, "\"name\"");
        List<String> roles = extractRoles(payloadJson);
        if (userId <= 0) {
            return null;
        }
        return new AuthContext(userId, name, roles);
    }

    private static byte[] sign(String input, String secret) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    private static String base64Url(byte[] data) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String base64Url(String s) {
        return base64Url(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toJsonArray(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "[]";
        }
        return roles.stream().map(r -> "\"" + escape(r) + "\"").reduce((a, b) -> a + "," + b).map(s -> "[" + s + "]").orElse("[]");
    }

    private static long extractLong(String json, String key) {
        int idx = json.indexOf(key + ":");
        if (idx < 0) {
            return -1;
        }
        int start = idx + key.length() + 1;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String extractString(String json, String key) {
        int idx = json.indexOf(key + ":\"");
        if (idx < 0) {
            return "";
        }
        int start = idx + key.length() + 2;
        int end = json.indexOf('"', start);
        if (end < 0) {
            return "";
        }
        return json.substring(start, end);
    }

    private static List<String> extractRoles(String json) {
        int idx = json.indexOf("\"roles\":[");
        if (idx < 0) {
            return List.of();
        }
        int start = idx + "\"roles\":[".length();
        int end = json.indexOf(']', start);
        if (end < 0) {
            return List.of();
        }
        String body = json.substring(start, end).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(body.split(","))
                .map(s -> s.replace("\"", "").trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
