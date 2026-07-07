package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Integer> {
    UserDetails findByLogin(String login);
    Optional<UsuarioEntity> findByLoginOrEmail(String login, String email);
    boolean existsByLoginAndIdNot(String login, Integer id);
}