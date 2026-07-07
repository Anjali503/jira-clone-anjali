package com.example.jira.dto;

public class UpdateProfileRequest {

    private String name;
    private String phone;
    private String group;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
}
