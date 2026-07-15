package com.example.jira.model;

import java.time.Instant;

/**
 * A single comment on an Issue. Previously comments were stored as raw
 * Strings with no author or timestamp attached — this made it impossible
 * to tell who said what, or when, once more than one person was working
 * on an issue.
 */
public class Comment {

    private String text;
    private String authorId;
    private String authorName;
    private Instant createdAt = Instant.now();

    public Comment() {
    }

    public Comment(String text, String authorId, String authorName) {
        this.text = text;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = Instant.now();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
