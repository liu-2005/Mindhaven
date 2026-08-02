package com.mindhaven.config;

import java.time.LocalDateTime;
import java.util.List;

import com.mindhaven.entity.Article;
import com.mindhaven.entity.ArticleTag;
import com.mindhaven.entity.User;
import com.mindhaven.entity.UserRole;
import com.mindhaven.repository.ArticleRepository;
import com.mindhaven.repository.ArticleTagRepository;
import com.mindhaven.repository.RoleRepository;
import com.mindhaven.repository.UserRepository;
import com.mindhaven.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 仅 local 环境：创建演示账号与示例文章，便于本地联调。
 * 生产环境不会加载该组件（不包含于迁移脚本）。
 */
@Component
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ArticleRepository articleRepository;
    private final ArticleTagRepository articleTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           ArticleRepository articleRepository,
                           ArticleTagRepository articleTagRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.articleRepository = articleRepository;
        this.articleTagRepository = articleTagRepository;
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
            if (!userRoleRepository.existsByUserId(admin.getId())) {
                bindRoles(admin, List.of("USER", "CONTENT_ADMIN", "SAFETY_ADMIN", "USER_ADMIN"));
            }

            User demo = ensureUser("demo", "demo@mindhaven.local", "demo123", "小晴用户");
            if (!userRoleRepository.existsByUserId(demo.getId())) {
                bindRoles(demo, List.of("USER"));
            }

            seedArticles(admin.getId());
            log.info("[local] 演示数据就绪：admin/admin123（管理员）、demo/demo123（普通用户）");
        } catch (Exception e) {
            log.warn("[local] 演示数据初始化失败（可忽略）：{}", e.getMessage());
        }
    }

    private User ensureUser(String username, String email, String password, String nickname) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setNickname(nickname);
            u.setStatus("ACTIVE");
            return userRepository.save(u);
        });
    }

    private void bindRoles(User user, List<String> codes) {
        for (String code : codes) {
            roleRepository.findByCode(code).ifPresent(role -> {
                try {
                    userRoleRepository.save(new UserRole(user.getId(), role.getId()));
                } catch (Exception ignored) {
                    // 重复角色忽略
                }
            });
        }
    }

    private void seedArticles(Long authorId) {
        if (articleRepository.count() > 0) {
            return;
        }
        seedArticle(authorId, 1L, "如何在繁忙中照顾自己的情绪", "介绍基础的情绪觉察与自我照顾方法。",
                "<p>情绪本身没有对错，它只是信号。每天花几分钟安静地感受当下的状态，是照顾自己的第一步。</p>"
                        + "<p><strong>小练习：</strong>写下今天三种真实的感受，不需要评判。</p>", List.of(1L, 9L));
        seedArticle(authorId, 2L, "认识焦虑：它为什么出现", "科普焦虑的常见来源与应对方向。",
                "<p>焦虑常常来自对未来的不确定感。了解触发它的情境，有助于我们把模糊的担心转化为可行动的小步骤。</p>", List.of(2L, 4L));
        seedArticle(authorId, 3L, "改善睡眠的五个小习惯", "关于睡眠健康的实用建议。",
                "<p>固定入睡时间、睡前减少屏幕使用、保持卧室安静，这些习惯会慢慢改善你的睡眠质量。</p>", List.of(5L, 8L));
        seedArticle(authorId, 4L, "建立健康的人际边界", "谈论如何在关系里照顾自己。",
                "<p>健康的关系需要边界。学会温和地说“不”，是成熟与自我尊重的一部分。</p>", List.of(11L));
        seedArticle(authorId, 5L, "正念冥想入门指南", "用简单的方式开启正念练习。",
                "<p>正念练习不是清空大脑，而是学习观察念头而不被卷走。可以从每天三分钟的呼吸觉察开始。</p>", List.of(6L, 7L));
        seedArticle(authorId, 6L, "关于心理健康的基本科普", "澄清关于心理健康的常见误解。",
                "<p>心理健康和身体健康一样需要被关注。求助专业人士是勇敢且有效的行为，不是软弱。</p>", List.of(9L, 10L));
    }

    private void seedArticle(Long authorId, Long categoryId, String title, String summary, String content, List<Long> tagIds) {
        Article a = new Article();
        a.setCategoryId(categoryId);
        a.setAuthorUserId(authorId);
        a.setTitle(title);
        a.setSummary(summary);
        a.setContentHtml(content);
        a.setStatus("PUBLISHED");
        a.setViewCount(0L);
        a.setPublishedAt(LocalDateTime.now().minusDays((long) (6 - categoryId)));
        a.setVersion(0);
        a = articleRepository.save(a);
        for (Long tagId : tagIds) {
            try {
                articleTagRepository.save(new ArticleTag(a.getId(), tagId));
            } catch (Exception ignored) {
                // ignore duplicates
            }
        }
    }
}
