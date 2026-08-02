package com.mindhaven.common;

public final class RequestIdHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    public static void set(String requestId) {
        HOLDER.set(requestId);
    }

    public static String get() {
        String id = HOLDER.get();
        return id != null ? id : "";
    }

    public static void clear() {
        HOLDER.remove();
    }
}
