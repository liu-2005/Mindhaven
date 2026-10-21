package com.mindhaven.repository;

import com.mindhaven.entity.RiskEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    java.util.Optional<RiskEvent> findFirstBySourceTypeAndSourceIdOrderByIdDesc(String sourceType, Long sourceId);

    long countByUserId(Long userId);

    @Query("select r from RiskEvent r " +
            "where (:status is null or r.status = :status) " +
            "and (:level is null or r.riskLevel = :level) " +
            "order by r.detectedAt desc, r.id desc")
    Page<RiskEvent> search(@Param("status") String status, @Param("level") String level, Pageable pageable);
}
