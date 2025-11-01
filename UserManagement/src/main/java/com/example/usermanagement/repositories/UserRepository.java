package com.example.usermanagement.repositories;

import com.example.usermanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(String role);

    @Query(value = "SELECT u FROM User u WHERE u.role = :role")
    List<User> findUsersByRole(@Param("role") String role);

    boolean existsByUsername(String username);
}
