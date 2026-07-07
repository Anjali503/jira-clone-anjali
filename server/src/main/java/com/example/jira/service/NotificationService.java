package com.example.jira.service;

import com.example.jira.model.Notification;
import com.example.jira.model.User;
import com.example.jira.repository.NotificationRepository;
import com.example.jira.repository.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Create a persistent in-app notification and optionally send an email.
     *
     * @param userId           recipient user id
     * @param type             notification type (ASSIGNMENT, STATUS_CHANGE, etc.)
     * @param message          human-readable message
     * @param deduplicationKey stable key; if a notification with this key already
     *                         exists it will not be created again
     */
    public void notifyUser(String userId, String type, String message, String deduplicationKey) {
        if (userId == null || userId.isBlank()) {
            log.warn("Notification skipped – no userId provided. message={}", message);
            return;
        }

        // Deduplication check
        if (notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
            log.debug("Duplicate notification skipped. key={}", deduplicationKey);
            return;
        }

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setMessage(message);
        n.setDeduplicationKey(deduplicationKey);
        notificationRepository.save(n);
        log.info("In-app notification saved for user={} type={}", userId, type);

        // Email – only if user has emailNotificationsEnabled
        userRepository.findById(new ObjectId(userId)).ifPresent(user -> {
            if (user.isEmailNotificationsEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) {
                emailService.sendEmail(user.getEmail(), "Jira Notification: " + type, message);
            }
        });
    }

    /**
     * Retrieve all notifications for a user (newest first).
     */
    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Mark a notification as read.
     */
    public Notification markRead(String notificationId) {
        Notification n = notificationRepository.findById(new ObjectId(notificationId))
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setRead(true);
        return notificationRepository.save(n);
    }

    /**
     * Mark a notification as unread.
     */
    public Notification markUnread(String notificationId) {
        Notification n = notificationRepository.findById(new ObjectId(notificationId))
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setRead(false);
        return notificationRepository.save(n);
    }
}
