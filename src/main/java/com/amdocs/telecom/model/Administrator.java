package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class Administrator {

    private Long adminId;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Administrator() {
    }

    public Administrator(String username, String passwordHash, String firstName, String lastName,
                          String email, String status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Administrator{" +
                "adminId=" + adminId +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", lockedUntil=" + lockedUntil +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
