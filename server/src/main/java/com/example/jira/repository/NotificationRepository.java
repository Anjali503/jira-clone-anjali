package com.example.jira.repository;

import com.example.jira.model.Notification;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, ObjectId> {

    List<Notification> findByUserIdOrderByTimestampDesc(String userId);

    Optional<Notification> findByDeduplicationKey(String deduplicationKey);

    boolean existsByDeduplicationKey(String deduplicationKey);
}
