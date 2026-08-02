package com.mindhaven.repository;

import java.util.Optional;

import com.mindhaven.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @Query("select s from ChatSession s where s.userId = :userId and s.deletedAt is null order by s.lastMessageAt desc, s.id desc")
    Page<ChatSession> findByUserIdOrderByLastMessage(@Param("userId") Long userId, Pageable pageable);

    @Query("select s from ChatSession s where s.deletedAt is null order by s.lastMessageAt desc, s.id desc")
    Page<ChatSession> adminPage(Pageable pageable);

    long countByDeletedAtIsNull();

    @Query("select s from ChatSession s where s.deletedAt is null and s.createdAt >= :from and s.createdAt <= :to order by s.createdAt")
    java.util.List<ChatSession> findAllCreatedBetween(@Param("from") java.time.LocalDateTime from,
                                                      @Param("to") java.time.LocalDateTime to);
}
