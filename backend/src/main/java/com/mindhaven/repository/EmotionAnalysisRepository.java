package com.mindhaven.repository;

import java.util.List;
import java.util.Optional;

import com.mindhaven.entity.EmotionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionAnalysisRepository extends JpaRepository<EmotionAnalysis, Long> {

    Optional<EmotionAnalysis> findFirstBySourceTypeAndSourceIdAndCurrentResultOrderByIdDesc(String sourceType, Long sourceId, Boolean currentResult);

    List<EmotionAnalysis> findBySourceTypeAndSourceIdAndCurrentResultTrue(String sourceType, Long sourceId);

    void deleteBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
