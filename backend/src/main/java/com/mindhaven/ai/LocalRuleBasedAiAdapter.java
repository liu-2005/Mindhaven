package com.mindhaven.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * 本地规则式 AI 适配器（仅用于本地开发联调）。
 * 规则集中在此处，便于替换为真实模型；前端协议不随供应商变化。
 */
@Component
public class LocalRuleBasedAiAdapter implements AiAdapter {

    private static final String MODEL_VERSION = "local-rule-v1";
    private static final String POLICY_VERSION = "policy-v1";

    private static final Set<String> CRISIS_KEYWORDS = Set.of(
            "自杀", "不想活", "想去死", "结束生命", "活不下去", "没有活下去", "离开这个世界", "想死");

    private static final Set<String> WARNING_KEYWORDS = Set.of(
            "绝望", "没有希望", "撑不住", "熬不下去", "伤害自己", "自残");

    private static final Set<String> CONCERN_KEYWORDS = Set.of(
            "焦虑", "害怕", "恐惧", "担忧", "睡不好", "失眠", "压力", "难过", "抑郁", "崩溃", "烦躁");

    private static final Map<String, String> TOPIC_REPLIES = Map.ofEntries(
            Map.entry("睡", "我能感觉到睡眠困扰让你很疲惫。试着放下手机，做几次深呼吸，今晚只记录感受、不评判自己。"),
            Map.entry("压力", "听起来最近的压力不小。允许自己先放下一点期待，把一件大事拆成今天能做的一小步。"),
            Map.entry("工作", "工作带来的紧绷常常需要出口。写下来、说出来，或者暂时离开桌面散步几分钟，都是一种照顾自己的方式。"),
            Map.entry("朋友", "人际关系有时让人既渴望又疲惫。照顾好自己的感受同样重要，慢慢来。"),
            Map.entry("累", "你很辛苦了。此刻不必强撑着，停下来休息不是软弱，而是恢复的力量。"),
            Map.entry("难过", "难过的时候不需要马上好起来。我愿意在这里陪着你，一件一件听你说完。"),
            Map.entry("焦虑", "焦虑常常在提醒我们很在乎。先做一次慢慢吸气、缓缓呼气的练习，我们把担心的事逐条理一理。"),
            Map.entry("哭", "想哭就哭一会儿吧，眼泪也是情绪在说话。我在这里陪着你。"),
            Map.entry("家", "家庭带来的感受往往很深。你不需要独自消化一切，可以慢慢表达，我随时都在。"));

    private static final List<String> GENERIC_REPLIES = List.of(
            "我在听。你能愿意说出来，这本身就是很勇敢的一步。",
            "谢谢你愿意和我分享这些。此刻的感觉是怎样的？",
            "听起来你今天经历了不少。要不要再多和我说说细节？",
            "情绪没有对错，它只是信号。我们可以一起看看它在提醒什么。",
            "我陪着你。不用急着给今天下结论，慢慢来就好。");

    private static final String CRISIS_GUIDANCE = "我看到你正在经历非常困难的时刻。你的感受很重要，请不要独自承受。"
            + "请立即联系你信任的亲友，或拨打心理援助热线，也可以直接前往就近的医院急诊科寻求帮助。"
            + "全国心理援助热线：12356；紧急情况请拨打 110/120。你不是一个人，我们都在乎你。";

    @Override
    public ChatReply reply(String userMessage, List<ChatTurn> history) {
        String riskLevel = "NORMAL";
        List<String> triggerCodes = List.of();
        String guidanceCode = null;

        if (containsAny(userMessage, CRISIS_KEYWORDS)) {
            riskLevel = "CRISIS";
            triggerCodes = List.of("CRISIS_SELF_HARM");
            guidanceCode = "IMMEDIATE_SUPPORT";
            return new ChatReply(CRISIS_GUIDANCE, riskLevel, triggerCodes, guidanceCode);
        }
        if (containsAny(userMessage, WARNING_KEYWORDS)) {
            riskLevel = "WARNING";
            triggerCodes = List.of("WARNING_HOPELESSNESS");
            guidanceCode = "LOCAL_SUPPORT";
        } else if (containsAny(userMessage, CONCERN_KEYWORDS)) {
            riskLevel = "CONCERN";
            triggerCodes = List.of("CONCERN_NEGATIVE_AFFECT");
            guidanceCode = null;
        }

        String reply = pickReply(userMessage);
        if (riskLevel.equals("WARNING")) {
            reply = reply + "\n\n如果你最近常常感到撑不住，请优先联系信任的人或拨打心理援助热线 12356。";
        }
        return new ChatReply(reply, riskLevel, triggerCodes, guidanceCode);
    }

    @Override
    public EmotionAnalysisResult analyzeEmotion(String text) {
        if (text == null || text.isBlank()) {
            return new EmotionAnalysisResult(50, "UNKNOWN", false, "NORMAL",
                    "暂未获取到足够内容进行分析。", "试着写下今天最真实的感受。",
                    List.of("记录此刻情绪", "做一次深呼吸"), List.of());
        }
        if (containsAny(text, CRISIS_KEYWORDS)) {
            return new EmotionAnalysisResult(8, "CRISIS", true, "CRISIS",
                    "检测到即时危机信号，需要立即寻求现实世界支持。", CRISIS_GUIDANCE,
                    List.of("联系信任的人", "拨打心理援助热线 12356"), List.of("危机", "绝望"));
        }
        if (containsAny(text, WARNING_KEYWORDS)) {
            return new EmotionAnalysisResult(25, "SEVERE_DISTRESS", true, "WARNING",
                    "检测到持续的强烈消极表达，建议尽快向可信任的人或专业人士求助。",
                    "先放慢呼吸，并考虑向信任的人说明近况。",
                    List.of("短暂离开屏幕", "给信任的人发一条消息"), List.of("绝望", "无助"));
        }
        if (containsAny(text, CONCERN_KEYWORDS)) {
            int score = 35 + text.length() % 15;
            return new EmotionAnalysisResult(score, "ANXIOUS", true, "CONCERN",
                    "检测到明显的消极情绪表达，但未发现即时危机信号。",
                    "先尝试放慢呼吸，并考虑向可信任的人说明近况。",
                    List.of("短暂离开屏幕", "记录今晚的睡眠情况"), List.of("焦虑", "压力"));
        }
        return new EmotionAnalysisResult(72, "CALM", false, "NORMAL",
                "当前表达整体平稳，情绪状态良好。", "保持这样的觉察，继续照顾好自己的节奏。",
                List.of("保持规律作息", "记录一件小确幸"), List.of("平静", "放松"));
    }

    @Override
    public String providerName() {
        return "local-rules";
    }

    @Override
    public String modelVersion() {
        return MODEL_VERSION;
    }

    public String policyVersion() {
        return POLICY_VERSION;
    }

    private String pickReply(String message) {
        for (Map.Entry<String, String> e : TOPIC_REPLIES.entrySet()) {
            if (message.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return GENERIC_REPLIES.get(Math.floorMod(message.hashCode(), GENERIC_REPLIES.size()));
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
