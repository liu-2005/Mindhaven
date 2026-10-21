package com.mindhaven.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import com.mindhaven.entity.Article;
import com.mindhaven.entity.ArticleTag;
import com.mindhaven.entity.ArticleViewEvent;
import com.mindhaven.entity.ChatMessage;
import com.mindhaven.entity.ChatSession;
import com.mindhaven.entity.EmotionAnalysis;
import com.mindhaven.entity.EmotionDiary;
import com.mindhaven.entity.FileObject;
import com.mindhaven.entity.RiskEvent;
import com.mindhaven.entity.User;
import com.mindhaven.entity.UserRole;
import com.mindhaven.repository.ArticleRepository;
import com.mindhaven.repository.ArticleTagRepository;
import com.mindhaven.repository.ArticleViewEventRepository;
import com.mindhaven.repository.ChatMessageRepository;
import com.mindhaven.repository.ChatSessionRepository;
import com.mindhaven.repository.EmotionAnalysisRepository;
import com.mindhaven.repository.EmotionDiaryRepository;
import com.mindhaven.repository.RiskEventRepository;
import com.mindhaven.repository.RoleRepository;
import com.mindhaven.repository.UserRepository;
import com.mindhaven.repository.UserRoleRepository;
import com.mindhaven.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 仅 local 环境：生成丰富的演示数据（用户、日记、会话、文章、风险事件、封面图片）。
 * 通过标题/用户名等存在性判断保证可重复执行而不重复插入。生产环境不会加载。
 */
@Component
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String CRISIS_GUIDANCE = "我看到你正在经历非常困难的时刻。你的感受很重要，请不要独自承受。"
            + "请立即联系你信任的亲友，或拨打心理援助热线 12356，也可以直接前往就近的医院急诊科寻求帮助。"
            + "紧急情况请拨打 110/120。你不是一个人，我们都在乎你。";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ArticleRepository articleRepository;
    private final ArticleTagRepository articleTagRepository;
    private final ArticleViewEventRepository viewEventRepository;
    private final EmotionDiaryRepository diaryRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final EmotionAnalysisRepository analysisRepository;
    private final RiskEventRepository riskEventRepository;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           ArticleRepository articleRepository,
                           ArticleTagRepository articleTagRepository,
                           ArticleViewEventRepository viewEventRepository,
                           EmotionDiaryRepository diaryRepository,
                           ChatSessionRepository sessionRepository,
                           ChatMessageRepository messageRepository,
                           EmotionAnalysisRepository analysisRepository,
                           RiskEventRepository riskEventRepository,
                           FileService fileService,
                           PasswordEncoder passwordEncoder,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.articleRepository = articleRepository;
        this.articleTagRepository = articleTagRepository;
        this.viewEventRepository = viewEventRepository;
        this.diaryRepository = diaryRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.analysisRepository = analysisRepository;
        this.riskEventRepository = riskEventRepository;
        this.fileService = fileService;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    public void run(String... args) {
        if (!appProperties.isSeedDemoData()) {
            return;
        }
        try {
            User admin = ensureUser("admin", "admin@mindhaven.local", "admin123", "系统管理员");
            bindRoles(admin, List.of("USER", "CONTENT_ADMIN", "SAFETY_ADMIN", "USER_ADMIN"));

            User demo = ensureUser("demo", "demo@mindhaven.local", "demo123", "小晴");
            bindRoles(demo, List.of("USER"));
            User xiaomei = ensureUser("xiaomei", "xiaomei@mindhaven.local", "demo123", "小美");
            bindRoles(xiaomei, List.of("USER"));
            User ajie = ensureUser("ajie", "ajie@mindhaven.local", "demo123", "阿杰");
            bindRoles(ajie, List.of("USER"));
            User anran = ensureUser("anran", "anran@mindhaven.local", "demo123", "安然");
            bindRoles(anran, List.of("USER"));

            List<Long> articleIds = seedArticles(admin.getId());
            seedDiaries(List.of(demo.getId(), xiaomei.getId(), ajie.getId(), anran.getId()));
            Long riskUserId = demo.getId();
            seedSessionsAndRisk(riskUserId);
            if (viewEventRepository.count() == 0) {
                seedViewEvents(articleIds);
            }
            log.info("[local] 演示数据已就绪：admin/admin123、demo/demo123 等 5 个账号，"
                    + "文章 {} 篇（含封面图）、日记、会话与风险事件已生成", articleIds.size());
        } catch (Exception e) {
            log.warn("[local] 演示数据初始化失败（可忽略）：{}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- users

    private User ensureUser(String username, String email, String password, String nickname) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setNickname(nickname);
            u.setStatus("ACTIVE");
            u.setConsentedAt(LocalDateTime.now());
            u.setPrivacyConsentVersion("2026-08-01");
            return userRepository.save(u);
        });
    }

    private void bindRoles(User user, List<String> codes) {
        if (userRoleRepository.existsByUserId(user.getId())) {
            return;
        }
        for (String code : codes) {
            roleRepository.findByCode(code).ifPresent(role -> userRoleRepository.save(new UserRole(user.getId(), role.getId())));
        }
    }

    // ---------------------------------------------------------------- diaries

    private void seedDiaries(List<Long> userIds) {
        Random random = new Random(20260802L);
        String[] emotionsHappy = {"开心", "平静", "期待"};
        String[] emotionsLow = {"难过", "焦虑", "疲惫"};
        String[][] notes = {
                {"完成了手头的一件重要工作，感觉踏实了一些。", "和好友聊了会儿天，心情放松了不少。", "今天天气很好，出门走了走，头脑清醒了很多。"},
                {"早上心情不错，午后有点小波动，但及时调整了。", "读到一段很温暖的话，记录下来慢慢回味。", "今天效率不错，给自己点了杯喜欢的咖啡。"},
                {"今天有些低落，什么都不太想动。", "遇到一点不顺心的事，情绪涌上来，允许自己待了一会儿。", "身体很累，早早就想躺下休息。"},
                {"把烦恼写下来之后，好像没那么重了。", "学着做了一次深呼吸练习，紧张感减轻了。", "虽然结果不完美，但我尽力了，这值得肯定。"},
        };
        for (Long userId : userIds) {
            if (!diaryRepository.findUserDiaries(userId, null, null).isEmpty()) {
                continue;
            }
            int startOffset = 0;
            LocalDate today = LocalDate.now();
            int count = 0;
            for (int offset = 0; offset < 45 && count < 18; offset += random.nextInt(3) + 1) {
                LocalDate date = today.minusDays(offset);
                int mood = random.nextInt(10) + 1;
                String[] emotionPool = mood >= 7 ? emotionsHappy : mood >= 4 ? emotionsHappy : emotionsLow;
                String emotion = emotionPool[random.nextInt(emotionPool.length)];
                String[] notePool = mood >= 7 ? notes[0] : mood >= 5 ? notes[1] : notes[2];
                String note = notePool[random.nextInt(notePool.length)];

                EmotionDiary diary = new EmotionDiary();
                diary.setUserId(userId);
                diary.setDiaryDate(date);
                diary.setMoodScore(mood);
                diary.setDominantEmotion(emotion);
                diary.setEmotionTriggers(mood >= 7 ? "生活进展顺利，遇到了让人开心的事"
                        : mood >= 5 ? "工作/学习平稳，偶有小波动" : "感到压力或疲惫，情绪有些低落");
                diary.setContent(note);
                diary.setSleepQuality(random.nextInt(5) + 1);
                diary.setStressLevel(random.nextInt(5) + 1);
                diary.setVersion(0);
                diaryRepository.save(diary);
                count++;
            }
        }
    }

    // ---------------------------------------------------------------- chat & risk

    private void seedSessionsAndRisk(Long userId) {
        if (!sessionRepository.findByUserIdOrderByLastMessage(userId, PageRequest.of(0, 1)).isEmpty()) {
            return;
        }
        ChatSession s1 = new ChatSession();
        s1.setUserId(userId);
        s1.setTitle("最近总是睡不好");
        s1.setStatus("ACTIVE");
        s1.setMessageCount(0);
        s1 = sessionRepository.save(s1);
        ChatMessage s1u1 = saveMessage(s1.getId(), "USER", "你好，我最近总是睡不好，凌晨总会醒。");
        saveMessage(s1.getId(), "ASSISTANT", "我能感觉到睡眠困扰让你很疲惫。试着固定入睡时间、睡前一小时放下手机，记录一下今天的睡眠情况，会慢慢好起来的。");
        ChatMessage s1u2 = saveMessage(s1.getId(), "USER", "工作压力也很大，总是担心做不完。");
        saveMessage(s1.getId(), "ASSISTANT", "听起来最近的压力不小。允许自己先放下一点期待，把一件大事拆成今天能完成的一小步，先做起来。");
        s1.setMessageCount(4);
        s1.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(s1);
        saveAnalysis(s1.getId(), 45, "ANXIOUS", true, "CONCERN",
                "检测到持续的焦虑与睡眠困扰，建议尝试放松练习并保持规律作息。",
                "先做几次深呼吸，把今天最重要的一件事记下来。", List.of("呼吸练习", "记录睡眠"));

        ChatSession s2 = new ChatSession();
        s2.setUserId(userId);
        s2.setTitle("我觉得很孤独");
        s2.setStatus("ACTIVE");
        s2.setMessageCount(0);
        s2 = sessionRepository.save(s2);
        saveMessage(s2.getId(), "USER", "有时候我觉得一个人很孤单，不想说话。");
        saveMessage(s2.getId(), "ASSISTANT", "我在听。你能愿意说出来，这本身就是很勇敢的一步。孤单的时候，先照顾好眼前的一顿饭、一杯水。");
        ChatMessage s2u3 = saveMessage(s2.getId(), "USER", "我甚至想过如果不用活下去了会不会轻松一点。");
        saveMessage(s2.getId(), "ASSISTANT", CRISIS_GUIDANCE);
        s2.setMessageCount(4);
        s2.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(s2);
        saveAnalysis(s2.getId(), 10, "CRISIS", true, "CRISIS",
                "检测到即时危机信号，需要立即寻求现实世界支持。", CRISIS_GUIDANCE, List.of("联系信任的人", "拨打心理援助热线 12356"));

        if (riskEventRepository.countByUserId(userId) == 0) {
            RiskEvent crisis = new RiskEvent();
            crisis.setUserId(userId);
            crisis.setSourceType("CHAT_MESSAGE");
            crisis.setSourceId(s2u3.getId());
            crisis.setRiskLevel("CRISIS");
            crisis.setTriggerCodesJson("[\"CRISIS_SELF_HARM\"]");
            crisis.setRedactedSummary("用户表达自我伤害想法");
            crisis.setPolicyVersion("policy-v1");
            crisis.setModelVersion("local-rule-v1");
            crisis.setStatus("OPEN");
            riskEventRepository.save(crisis);

            RiskEvent concern = new RiskEvent();
            concern.setUserId(userId);
            concern.setSourceType("CHAT_MESSAGE");
            concern.setSourceId(s1u1.getId());
            concern.setRiskLevel("CONCERN");
            concern.setTriggerCodesJson("[\"CONCERN_NEGATIVE_AFFECT\"]");
            concern.setRedactedSummary("用户表达睡眠困扰与压力");
            concern.setPolicyVersion("policy-v1");
            concern.setModelVersion("local-rule-v1");
            concern.setStatus("OPEN");
            riskEventRepository.save(concern);
        }
    }

    private ChatMessage saveMessage(Long sessionId, String senderType, String content) {
        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setSenderType(senderType);
        m.setContent(content);
        m.setStatus("COMPLETED");
        if ("ASSISTANT".equals(senderType)) {
            m.setFinishReason("STOP");
            m.setModelProvider("local-rules");
            m.setModelVersion("local-rule-v1");
            m.setCompletedAt(LocalDateTime.now());
        }
        return messageRepository.save(m);
    }

    private void saveAnalysis(Long sessionId, int score, String label, boolean negative, String riskLevel,
                              String riskDescription, String suggestion, List<String> improvements) {
        EmotionAnalysis analysis = new EmotionAnalysis();
        analysis.setSourceType("CHAT_SESSION");
        analysis.setSourceId(sessionId);
        analysis.setEmotionScore(score);
        analysis.setLabel(label);
        analysis.setIsNegative(negative);
        analysis.setRiskLevel(riskLevel);
        analysis.setRiskDescription(riskDescription);
        analysis.setSuggestion(suggestion);
        analysis.setImprovementSuggestionsJson(toJson(improvements));
        analysis.setModelProvider("local-rules");
        analysis.setModelVersion("local-rule-v1");
        analysis.setPolicyVersion("policy-v1");
        analysis.setStatus("SUCCEEDED");
        analysis.setCurrentResult(true);
        analysis.setAnalyzedAt(LocalDateTime.now());
        analysisRepository.save(analysis);
    }

    private String toJson(List<String> list) {
        return list.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").reduce((a, b) -> a + "," + b)
                .map(s -> "[" + s + "]").orElse("[]");
    }

    // ---------------------------------------------------------------- articles & covers

    private List<Long> seedArticles(Long authorId) {
        List<Long> ids = new java.util.ArrayList<>();
        for (SeedArticle spec : articleSpecs()) {
            Article article = articleRepository.findByTitleAndDeletedAtIsNull(spec.title()).orElse(null);
            if (article == null) {
                article = new Article();
                article.setCategoryId(spec.categoryId());
                article.setAuthorUserId(authorId);
                article.setTitle(spec.title());
                article.setSummary(spec.summary());
                article.setContentHtml(spec.content());
                article.setStatus("PUBLISHED");
                article.setViewCount(0L);
                article.setPublishedAt(LocalDateTime.now().minusDays((long) (8 - spec.categoryId())));
                article.setVersion(0);
                article = articleRepository.save(article);
                for (Long tagId : spec.tagIds()) {
                    articleTagRepository.save(new ArticleTag(article.getId(), tagId));
                }
                ids.add(article.getId());
            }
            if (article.getCoverFileId() == null) {
                attachCover(article, spec);
            }
        }
        return ids;
    }

    private void attachCover(Article article, SeedArticle spec) {
        if (article.getCoverFileId() != null) {
            return;
        }
        String svg = coverSvg(spec.title(), spec.emoji(), spec.color1(), spec.color2());
        FileObject cover = fileService.store(article.getAuthorUserId(),
                svg.getBytes(StandardCharsets.UTF_8),
                "cover-" + article.getId() + ".svg",
                "image/svg+xml", "ARTICLE", article.getId(), "cover");
        article.setCoverFileId(cover.getId());
        articleRepository.save(article);
    }

    private String coverSvg(String title, String emoji, String color1, String color2) {
        String safeTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"500\" viewBox=\"0 0 800 500\">\n"
                + "  <defs>\n"
                + "    <linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">\n"
                + "      <stop offset=\"0\" stop-color=\"" + color1 + "\"/>\n"
                + "      <stop offset=\"1\" stop-color=\"" + color2 + "\"/>\n"
                + "    </linearGradient>\n"
                + "  </defs>\n"
                + "  <rect width=\"800\" height=\"500\" fill=\"url(#g)\"/>\n"
                + "  <circle cx=\"680\" cy=\"80\" r=\"150\" fill=\"rgba(255,255,255,0.12)\"/>\n"
                + "  <circle cx=\"110\" cy=\"440\" r=\"130\" fill=\"rgba(255,255,255,0.10)\"/>\n"
                + "  <circle cx=\"400\" cy=\"250\" r=\"175\" fill=\"rgba(255,255,255,0.06)\"/>\n"
                + "  <text x=\"400\" y=\"225\" font-family=\"'Segoe UI Emoji','Apple Color Emoji',sans-serif\" font-size=\"112\" text-anchor=\"middle\">" + emoji + "</text>\n"
                + "  <text x=\"400\" y=\"360\" font-family=\"'PingFang SC','Microsoft YaHei',sans-serif\" font-size=\"38\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\">" + safeTitle + "</text>\n"
                + "  <text x=\"400\" y=\"410\" font-family=\"'PingFang SC','Microsoft YaHei',sans-serif\" font-size=\"20\" fill=\"rgba(255,255,255,0.88)\" text-anchor=\"middle\">心晴 MindHaven · 心理健康科普</text>\n"
                + "</svg>\n";
    }

    private List<SeedArticle> articleSpecs() {
        return List.of(
                new SeedArticle(1L, "如何在繁忙中照顾自己的情绪", "介绍基础的情绪觉察与自我照顾方法。",
                        "<p>情绪本身没有对错，它只是信号。每天花几分钟安静地感受当下的状态，是照顾自己的第一步。</p><p><strong>小练习：</strong>写下今天三种真实的感受，不需要评判。</p>",
                        "🌿", "#14b8a6", "#f97316", List.of(1L, 9L)),
                new SeedArticle(1L, "认识情绪信号：当大脑在报警", "理解情绪背后的信息，学会与它相处。",
                        "<p>焦虑、愤怒、悲伤都在传递信息：它们在提醒你哪些需求被忽略了。</p><p>试着用“我感到……因为我需要……”的句式，把情绪翻译成需求。</p>",
                        "🧠", "#0d9488", "#22d3ee", List.of(1L, 9L)),
                new SeedArticle(2L, "认识焦虑：它为什么出现", "科普焦虑的常见来源与应对方向。",
                        "<p>焦虑常常来自对未来的不确定感。了解触发它的情境，有助于我们把模糊的担心转化为可行动的小步骤。</p>",
                        "😰", "#8b5cf6", "#f97316", List.of(2L, 4L)),
                new SeedArticle(2L, "把大压力拆成小步：实用减压法", "当压力很大时，如何一步步恢复掌控感。",
                        "<p>压力大的时候，任务清单可能已经失控。先把目标缩小到“今天能完成的一小步”，行动本身就会降低不确定感。</p><p>每天安排 10 分钟“放下时间”：散步、拉伸或单纯发呆。</p>",
                        "🪜", "#7c3aed", "#f59e0b", List.of(2L, 4L, 12L)),
                new SeedArticle(3L, "改善睡眠的五个小习惯", "关于睡眠健康的实用建议。",
                        "<p>固定入睡时间、睡前减少屏幕使用、保持卧室安静，这些习惯会慢慢改善你的睡眠质量。</p>",
                        "🌙", "#6366f1", "#22d3ee", List.of(5L, 8L)),
                new SeedArticle(3L, "睡前的静心仪式", "用简单的仪式帮身体进入休息状态。",
                        "<p>睡前 30 分钟调暗灯光、关掉通知，做几次缓慢的腹式呼吸，让交感神经慢慢降速。</p>",
                        "🛏️", "#4f46e5", "#67e8f9", List.of(5L, 6L)),
                new SeedArticle(4L, "建立健康的人际边界", "谈论如何在关系里照顾自己。",
                        "<p>健康的关系需要边界。学会温和地说“不”，是成熟与自我尊重的一部分。</p>",
                        "🤝", "#f43f5e", "#fb923c", List.of(11L)),
                new SeedArticle(4L, "在关系里被倾听的方法", "如何在表达与倾听之间找到平衡。",
                        "<p>表达前先确认自己的需要，倾听时放下评判。被听见的感觉，往往从一次真诚的“我听到你了”开始。</p>",
                        "🫂", "#e11d48", "#f97316", List.of(11L)),
                new SeedArticle(5L, "正念冥想入门指南", "用简单的方式开启正念练习。",
                        "<p>正念练习不是清空大脑，而是学习观察念头而不被卷走。可以从每天三分钟的呼吸觉察开始。</p>",
                        "🧘", "#10b981", "#84cc16", List.of(6L, 7L)),
                new SeedArticle(5L, "每天十分钟：微习惯的力量", "小到不可能失败的习惯改变策略。",
                        "<p>比起宏伟目标，每天十分钟的微小行动更能持续。把它们和已有习惯绑定，坚持会变得毫不费力。</p>",
                        "⏳", "#059669", "#a3e635", List.of(10L, 13L)),
                new SeedArticle(6L, "关于心理健康的基本科普", "澄清关于心理健康的常见误解。",
                        "<p>心理健康和身体健康一样需要被关注。求助专业人士是勇敢且有效的行为，不是软弱。</p>",
                        "📖", "#0ea5e9", "#14b8a6", List.of(9L, 10L)),
                new SeedArticle(6L, "心理求助不可耻：什么时候该找专业帮助", "识别需要专业支持的信号。",
                        "<p>当情绪长时间影响工作与生活、出现自我伤害念头、或睡眠食欲明显改变时，寻求心理咨询或精神科帮助是合理的下一步。</p>",
                        "🩺", "#0284c7", "#38bdf8", List.of(3L, 9L)));
    }

    private record SeedArticle(Long categoryId, String title, String summary, String content,
                               String emoji, String color1, String color2, List<Long> tagIds) {
    }

    // ---------------------------------------------------------------- view events

    private void seedViewEvents(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return;
        }
        Random random = new Random(7L);
        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < 30; offset++) {
            LocalDate date = today.minusDays(offset);
            int events = random.nextInt(6);
            for (int i = 0; i < events; i++) {
                Long articleId = articleIds.get(random.nextInt(articleIds.size()));
                ArticleViewEvent event = new ArticleViewEvent();
                event.setArticleId(articleId);
                event.setViewDate(date);
                viewEventRepository.save(event);
            }
        }
    }
}
