package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    UserDetails findByLogin(String login);
}