package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Integer> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
}
