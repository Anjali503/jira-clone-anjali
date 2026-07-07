package com.example.jira.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "work_logs")
public class WorkLog {
    @Id
    private ObjectId id;

    @Indexed
    private ObjectId taskId; // references Issue._id

    private Instant date; // must be past or present

    private Double duration; // hours logged, non‑negative

    private String description;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    // Getters & Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getTaskId() { return taskId; }
    public void setTaskId(ObjectId taskId) { this.taskId = taskId; }

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
