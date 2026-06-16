package com.example.jira.service;

import com.example.jira.model.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Simple in‑memory notification store. In a real product this would be persisted
 * and a push channel would be used, but for the assignment a lightweight
 * service is sufficient.
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Notification entity – small POJO.
     */
    public static class Notification {
        public final String userId;
        public final String message;
        public final Instant timestamp;
        public Notification(String userId, String message) {
            this.userId = userId;
            this.message = message;
            this.timestamp = Instant.now();
        }
    }

    // Thread‑safe list for demo purposes.
    private final List<Notification> store = new CopyOnWriteArrayList<>();

    /**
     * Record a notification for a user.
     */
    public void notifyUser(String userId, String message) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempted to notify without a userId – message ignored: {}", message);
            return;
        }
        Notification n = new Notification(userId, message);
        store.add(n);
        log.info("Notification for {}: {}", userId, message);
    }

    /**
     * Retrieve all notifications for a given user and clear them from the store.
     * This simple semantics works well with a short‑polling UI.
     */
    public List<Notification> fetchAndClear(String userId) {
        List<Notification> forUser = store.stream()
                .filter(n -> n.userId.equals(userId))
                .collect(Collectors.toList());
        // Remove fetched notifications
        store.removeIf(n -> n.userId.equals(userId));
        return forUser;
    }
}
