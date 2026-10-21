package com.mindhaven.security;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import com.mindhaven.common.BizException;
import static org.assertj.core.api.Assertions.*;

class RequestLimitsTests {
    @Test
    void limitsArePerIdentityAndRejectExcessRequests() {
        var limits = new RequestLimits();
        limits.check("user-a", 2, Duration.ofMinutes(1));
        limits.check("user-a", 2, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limits.check("user-a", 2, Duration.ofMinutes(1))).isInstanceOf(BizException.class);
        assertThatCode(() -> limits.check("user-b", 2, Duration.ofMinutes(1))).doesNotThrowAnyException();
    }
}
