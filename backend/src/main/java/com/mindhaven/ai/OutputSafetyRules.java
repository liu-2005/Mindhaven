package com.mindhaven.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Deterministic prefilter. The external adapter also performs a separate semantic review. */
public class OutputSafetyRules {
    public static final String VERSION = "output-policy-v1";
    private record Rule(String code, Pattern pattern) {}
    private static Rule rule(String code, String expression) {
        return new Rule(code, Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }
    private static final List<Rule> RULES = List.of(
            rule("HARM_INSTRUCTIONS", "(自杀|自残|自伤|伤害自己|杀人).{0,12}(方法|步骤|教程|操作指南)|(?:howto|steps?to)(?:kill|harm|hurt)(?:yourself|someone)"),
            rule("MEDICAL_DIAGNOSIS", "你(?:已经|肯定|确实|一定|就是|确诊)?(?:患有|患上|得了|被确诊).{0,16}(症|障碍|疾病)|诊断结果(?:是|为)|you(?:definitely)?have(?:depression|bipolardisorder|schizophrenia)"),
            rule("MEDICATION_ADVICE", "(?:自行|直接|马上|立刻)(?:停药|加药|换药)|(?:每天|每日|每晚|一次).{0,12}(?:服用|口服|吃).{0,20}\\d+(?:mg|毫克|片)|(?:take|increase).{0,20}\\d+mg"),
            rule("FALSE_IDENTITY", "我是(?:一名|你的|专业的)?(?:心理咨询师|精神科医生|心理医生|执业医师)|i(?:am|'m)(?:your|a)(?:licensed)?(?:therapist|psychiatrist|doctor)"),
            rule("DEPENDENCY", "(?:不要|别再)(?:告诉|联系|相信)(?:家人|朋友|医生|其他人)|只有我(?:能|才|会).{0,8}(?:理解|懂|帮助)你|不要寻求专业帮助|onlyican(?:understand|help)you"),
            rule("SENSITIVE_DATA_REQUEST", "(?:请|需要你|把你的|提供).{0,12}(?:身份证号|银行卡密码|登录密码|家庭详细地址|详细住址)|(?:sendme|giveme|provideyour).{0,15}(?:password|passportnumber|homeaddress)")
    );

    public AiAdapter.OutputReview review(String text) {
        if (text == null || text.isBlank() || text.length() > 24000) return new AiAdapter.OutputReview(false, List.of("INVALID_OUTPUT"));
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cf}\\s]", "").toLowerCase(java.util.Locale.ROOT);
        List<String> codes = new ArrayList<>();
        for (String clause : normalized.split("[。！？!?;；\\n]")) {
            // A clear refusal may name a prohibited topic without giving instructions.
            if (clause.matches("(?:我)?(?:不能|不会|无法|不)(?:为你)?提供.*")
                    && !clause.matches(".*(?:但|不过|however|but).*")) continue;
            for (Rule rule : RULES) if (rule.pattern().matcher(clause).find() && !codes.contains(rule.code())) codes.add(rule.code());
        }
        return new AiAdapter.OutputReview(codes.isEmpty(), List.copyOf(codes));
    }
}
