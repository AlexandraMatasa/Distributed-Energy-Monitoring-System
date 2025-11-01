package com.example.devicemanagement.controllers;

import com.example.devicemanagement.entities.UserCache;
import com.example.devicemanagement.repositories.DeviceUserAssignmentRepository;
import com.example.devicemanagement.repositories.UserCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/device/sync")
public class UserSyncController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncController.class);

    private final UserCacheRepository userCacheRepository;
    private final DeviceUserAssignmentRepository assignmentRepository;

    public UserSyncController(UserCacheRepository userCacheRepository,
                              DeviceUserAssignmentRepository assignmentRepository) {
        this.userCacheRepository = userCacheRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @PostMapping("/user-created")
    public ResponseEntity<Void> onUserCreated(@RequestBody UUID userId) {
        LOGGER.info("Received notification: user {} created", userId);

        UserCache cache = new UserCache();
        cache.setUserId(userId);
        userCacheRepository.save(cache);

        LOGGER.info("User {} added to cache", userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user-deleted")
    @Transactional
    public ResponseEntity<Void> onUserDeleted(@RequestBody UUID userId) {
        LOGGER.info("Received notification: user {} deleted", userId);

        userCacheRepository.deleteById(userId);
        LOGGER.info("User {} removed from cache", userId);

        assignmentRepository.deleteByUserId(userId);
        LOGGER.info("Deleted assignments for user {}", userId);

        return ResponseEntity.ok().build();
    }
}