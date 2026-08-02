package com.mindhaven.security;

public final class AuthContextHolder {

    private static final ThreadLocal<AuthContext> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(AuthContext ctx) {
        HOLDER.set(ctx);
    }

    public static AuthContext get() {
        return HOLDER.get();
    }

    public static Long userId() {
        AuthContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.userId();
    }

    public static AuthContext require() {
        AuthContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new com.mindhaven.common.BizException(com.mindhaven.common.ErrorCode.UNAUTHENTICATED);
        }
        return ctx;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
