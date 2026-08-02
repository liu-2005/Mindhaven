package com.mindhaven.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.mindhaven.entity.EmotionDiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmotionDiaryRepository extends JpaRepository<EmotionDiary, Long> {

    Optional<EmotionDiary> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    Optional<EmotionDiary> findByUserIdAndDiaryDateAndDeletedAtIsNull(Long userId, LocalDate diaryDate);

    boolean existsByUserIdAndDiaryDateAndDeletedAtIsNull(Long userId, LocalDate diaryDate);

    List<EmotionDiary> findByUserIdAndDeletedAtIsNullOrderByDiaryDateDesc(Long userId);

    @Query("select d from EmotionDiary d where d.userId = :userId and d.deletedAt is null " +
            "and (:fromDate is null or d.diaryDate >= :fromDate) and (:toDate is null or d.diaryDate <= :toDate) " +
            "order by d.diaryDate desc")
    List<EmotionDiary> findUserDiaries(@Param("userId") Long userId,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate);

    @Query("select d from EmotionDiary d where d.deletedAt is null " +
            "and (:userId is null or d.userId = :userId) " +
            "and (:minScore is null or d.moodScore >= :minScore) " +
            "and (:maxScore is null or d.moodScore <= :maxScore) " +
            "order by d.diaryDate desc, d.id desc")
    Page<EmotionDiary> adminPage(@Param("userId") Long userId,
                                 @Param("minScore") Integer minScore,
                                 @Param("maxScore") Integer maxScore,
                                 Pageable pageable);

    long countByDeletedAtIsNull();

    long countByDiaryDateAndDeletedAtIsNull(LocalDate diaryDate);

    @Query("select avg(d.moodScore) from EmotionDiary d where d.deletedAt is null")
    Double averageMoodScore();

    @Query("select d from EmotionDiary d where d.deletedAt is null and d.diaryDate >= :from and d.diaryDate <= :to order by d.diaryDate")
    java.util.List<EmotionDiary> findAllBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
