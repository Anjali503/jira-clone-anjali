package com.example.jira.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
public class Notification {

    @Id
    private ObjectId id;

    private String userId;
    private String message;
    private String type;          // ASSIGNMENT, STATUS_CHANGE, SPRINT_START, SPRINT_END, DUE_REMINDER
    private boolean read = false;
    private Instant timestamp = Instant.now();

    /**
     * Used for deduplication. Composed by the caller as a stable key that
     * uniquely identifies the logical event (e.g. "ASSIGNMENT:issueId:userId").
     * Before persisting, the service checks that no document with this key exists.
     */
    private String deduplicationKey;

    public String getId() {
        return id != null ? id.toHexString() : null;
    }

    public ObjectId getObjectId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }
}
