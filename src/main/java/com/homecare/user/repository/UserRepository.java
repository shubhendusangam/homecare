package com.homecare.user.repository;

import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:active IS NULL OR u.active = :active) AND " +
           "(:search IS NULL OR (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<User> findAllWithFilters(@Param("role") Role role,
                                  @Param("active") Boolean active,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role " +
           "AND u.createdAt >= :from AND u.createdAt < :to")
    long countByRoleAndCreatedAtBetween(@Param("role") Role role,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);
}

