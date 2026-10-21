package com.mindhaven.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import org.springframework.stereotype.Component;

/** Single-instance limits; replace with shared counters before deploying multiple instances. */
@Component
public class RequestLimits {
    private record Window(long expires, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    public void check(String key, int max, Duration period) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> entry.getValue().expires() <= now);
        if (windows.size() >= 10000 && !windows.containsKey(key)) throw new BizException(ErrorCode.RATE_LIMITED);
        windows.compute(key, (ignored, previous) -> {
            Window current = previous == null || previous.expires() <= now ? new Window(now + period.toMillis(), 0) : previous;
            if (current.count() >= max) throw new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试");
            return new Window(current.expires(), current.count() + 1);
        });
    }
}
