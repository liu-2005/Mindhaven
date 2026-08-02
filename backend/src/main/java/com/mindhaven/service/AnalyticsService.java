package com.mindhaven.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindhaven.entity.ChatSession;
import com.mindhaven.entity.EmotionDiary;
import com.mindhaven.entity.User;
import com.mindhaven.repository.ChatSessionRepository;
import com.mindhaven.repository.EmotionDiaryRepository;
import com.mindhaven.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final EmotionDiaryRepository diaryRepository;
    private final ChatSessionRepository sessionRepository;

    public AnalyticsService(UserRepository userRepository,
                            EmotionDiaryRepository diaryRepository,
                            ChatSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.diaryRepository = diaryRepository;
        this.sessionRepository = sessionRepository;
    }

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userRepository.countActiveUsers());
        data.put("totalDiaries", diaryRepository.countByDeletedAtIsNull());
        data.put("todayDiaries", diaryRepository.countByDiaryDateAndDeletedAtIsNull(today));
        data.put("totalSessions", sessionRepository.countByDeletedAtIsNull());
        Double avg = diaryRepository.averageMoodScore();
        data.put("averageMoodScore", avg == null ? 0.0 : Math.round(avg * 10) / 10.0);
        data.put("todaySessions", 0L);
        return data;
    }

    public List<Map<String, Object>> trends(LocalDate fromDate, LocalDate toDate, String granularity) {
        LocalDate from = fromDate == null ? LocalDate.now().minusDays(29) : fromDate;
        LocalDate to = toDate == null ? LocalDate.now() : toDate;
        if (from.isAfter(to)) {
            from = to;
        }

        List<EmotionDiary> diaries = diaryRepository.findAllBetween(from, to);
        List<ChatSession> sessions = sessionRepository.findAllCreatedBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusNanos(1));
        List<User> users = userRepository.findAllCreatedBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusNanos(1));

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> buckets = buckets(from, to, granularity);
        for (String bucket : buckets) {
            LocalDate bStart = LocalDate.parse(bucket);
            LocalDate bEnd = bucketEnd(bStart, granularity).minusDays(1);
            if (bEnd.isAfter(to)) {
                bEnd = to;
            }
            final LocalDate start = bStart;
            final LocalDate end = bEnd;
            long diariesInBucket = diaries.stream()
                    .filter(d -> !d.getDiaryDate().isBefore(start) && !d.getDiaryDate().isAfter(end))
                    .count();
            List<Integer> bucketScores = diaries.stream()
                    .filter(d -> !d.getDiaryDate().isBefore(start) && !d.getDiaryDate().isAfter(end))
                    .map(EmotionDiary::getMoodScore)
                    .toList();
            double avgMood = bucketScores.isEmpty() ? 0.0
                    : Math.round(bucketScores.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;
            long sessionsInBucket = sessions.stream()
                    .filter(s -> !s.getCreatedAt().toLocalDate().isBefore(start) && !s.getCreatedAt().toLocalDate().isAfter(end))
                    .count();
            long usersInBucket = users.stream()
                    .filter(u -> !u.getCreatedAt().toLocalDate().isBefore(start) && !u.getCreatedAt().toLocalDate().isAfter(end))
                    .count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", bucket);
            row.put("granularity", granularity);
            row.put("diaries", diariesInBucket);
            row.put("avgMoodScore", avgMood);
            row.put("sessions", sessionsInBucket);
            row.put("newUsers", usersInBucket);
            result.add(row);
        }
        return result;
    }

    private List<String> buckets(LocalDate from, LocalDate to, String granularity) {
        List<String> list = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            list.add(cursor.toString());
            cursor = bucketEnd(cursor, granularity);
        }
        return list;
    }

    private LocalDate bucketEnd(LocalDate start, String granularity) {
        return switch (granularity == null ? "day" : granularity) {
            case "week" -> start.plusWeeks(1);
            case "month" -> start.plusMonths(1);
            default -> start.plusDays(1);
        };
    }
}
