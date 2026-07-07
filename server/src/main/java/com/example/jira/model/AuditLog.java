package com.example.jira.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private ObjectId id;
    
    private String action; // UPDATE, DELETE
    
    private Instant timestamp = Instant.now();
    
    private String userId;
    
    private String entityId;
    
    private String entityType; // e.g., "WorkLog"
    
    private Map<String, Object> previousValues;
    
    private Map<String, Object> newValues;

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Map<String, Object> getPreviousValues() { return previousValues; }
    public void setPreviousValues(Map<String, Object> previousValues) { this.previousValues = previousValues; }

    public Map<String, Object> getNewValues() { return newValues; }
    public void setNewValues(Map<String, Object> newValues) { this.newValues = newValues; }
}
