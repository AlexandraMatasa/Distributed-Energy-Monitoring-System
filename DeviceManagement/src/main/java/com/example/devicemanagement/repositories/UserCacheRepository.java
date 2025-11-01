package com.example.devicemanagement.repositories;

import com.example.devicemanagement.entities.UserCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserCacheRepository extends JpaRepository<UserCache, UUID> {
}