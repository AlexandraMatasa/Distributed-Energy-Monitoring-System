package com.example.authmanagement.repositories;

import com.example.authmanagement.entities.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByUsername(String username);

    Optional<Credential> findByUserId(UUID userId);

    boolean existsByUsername(String username);

    @Transactional
    void deleteByUserId(UUID userId);
}