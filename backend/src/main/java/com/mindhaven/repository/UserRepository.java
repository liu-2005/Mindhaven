package com.mindhaven.repository;

import java.util.List;
import java.util.Optional;

import com.mindhaven.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select count(u) from User u where u.deletedAt is null")
    long countActiveUsers();

    @Query("select count(u) from User u where u.deletedAt is null and u.createdAt >= :from and u.createdAt <= :to")
    long countCreatedBetween(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("select u from User u where u.deletedAt is null and u.createdAt >= :from and u.createdAt <= :to")
    java.util.List<User> findAllCreatedBetween(@Param("from") java.time.LocalDateTime from,
                                               @Param("to") java.time.LocalDateTime to);

    @Query("select u from User u where u.deletedAt is null and (" +
            "(:keyword is null or u.username like %:keyword% or u.email like %:keyword% or u.nickname like %:keyword%))")
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
