package com.example.jira.dto;

import com.example.jira.model.User;

import java.time.Instant;

public class UserResponse {

    private String id;
    private String name;
    private String email;
    private String role;
    private String group;
    private String avatar;
    private String phone;
    private boolean emailNotificationsEnabled;
    private boolean active;
    private Instant createdAt;
    private Instant lastLoginAt;
    private Instant deactivatedAt;

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.name = user.getName();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.group = user.getGroup();
        response.avatar = user.getAvatar();
        response.phone = user.getPhone();
        response.emailNotificationsEnabled = user.isEmailNotificationsEnabled();
        response.active = user.isActive();
        response.createdAt = user.getCreatedAt();
        response.lastLoginAt = user.getLastLoginAt();
        response.deactivatedAt = user.getDeactivatedAt();
        return response;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getGroup() { return group; }
    public String getAvatar() { return avatar; }
    public String getPhone() { return phone; }
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
}
