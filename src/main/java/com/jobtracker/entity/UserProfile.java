package com.jobtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
public class UserProfile {
    @Id
    private Long id;

    @Column(name = "user_account_id", unique = true)
    private Long userAccountId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserProfile() {
    }

    public UserProfile(Long id, String name, String email, String phoneNumber) {
        this(id, id, name, email, phoneNumber);
    }

    public UserProfile(Long id, Long userAccountId, String name, String email,
                       String phoneNumber) {
        this.id = id;
        this.userAccountId = userAccountId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserAccountId() { return userAccountId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUserAccountId(Long userAccountId) { this.userAccountId = userAccountId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
