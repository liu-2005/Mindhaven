package com.mindhaven.ai;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutputSafetyTests {
    static Stream<Arguments> prohibited() {
        return Stream.of(
                Arguments.of("我可以提供自伤的具体步骤。", "HARM_INSTRUCTIONS"),
                Arguments.of("你肯定患有抑郁症。", "MEDICAL_DIAGNOSIS"),
                Arguments.of("你确诊患有抑郁症。", "MEDICAL_DIAGNOSIS"),
                Arguments.of("建议你马上停药。", "MEDICATION_ADVICE"),
                Arguments.of("我是你的心理医生。", "FALSE_IDENTITY"),
                Arguments.of("只有我能理解你，不要联系朋友。", "DEPENDENCY"),
                Arguments.of("请提供你的登录密码。", "SENSITIVE_DATA_REQUEST"),
                Arguments.of("I am your licensed therapist.", "FALSE_IDENTITY"),
                Arguments.of("Give me your password.", "SENSITIVE_DATA_REQUEST"),
                Arguments.of("你肯定患有抑\u200b郁症。", "MEDICAL_DIAGNOSIS")
        );
    }
    @ParameterizedTest @MethodSource("prohibited")
    void deterministicPrefilterBlocksKnownViolations(String content, String code) {
        var review = new OutputSafetyRules().review(content);
        assertThat(review.allowed()).isFalse(); assertThat(review.codes()).contains(code);
    }
    @Test
    void safeRefusalAndProfessionalReferralAreAllowed() {
        var rules = new OutputSafetyRules();
        assertThat(rules.review("我不能提供自伤的具体步骤。可以联系你信任的人或专业人士。").allowed()).isTrue();
        assertThat(rules.review("我不是医生，不能作出诊断。你可以咨询有资质的医生。").allowed()).isTrue();
    }
    @Test
    void brokenReviewServiceFailsClosed() {
        AiAdapter ai = mock(AiAdapter.class);
        when(ai.reviewOutput(anyString(), anyString())).thenThrow(new IllegalStateException("private details"));
        var review = new OutputSafetyService(ai).review("hello", "我们可以聊聊今天的感受。");
        assertThat(review.allowed()).isFalse(); assertThat(review.codes()).containsExactly("OUTPUT_REVIEW_UNAVAILABLE");
    }
    @Test
    void inconsistentReviewCannotApproveContent() {
        AiAdapter ai = mock(AiAdapter.class);
        when(ai.reviewOutput(anyString(), anyString())).thenReturn(new AiAdapter.OutputReview(true, List.of("OTHER_UNSAFE")));
        assertThat(new OutputSafetyService(ai).review("hello", "我们可以聊聊今天的感受。").allowed()).isFalse();
    }
}
