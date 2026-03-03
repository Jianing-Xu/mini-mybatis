package com.xujn.minimybatis.examples.phase2;

/**
 * Example result type used by phase 2 mapping scenarios.
 *
 * <p>Responsibility: expose a camel-case property so underscore-to-camel-case
 * result mapping can be verified by both tests and examples.
 *
 * <p>Thread-safety: mutable and not thread-safe.
 */
public class Phase2User {

    private Long id;
    private String userName;
    private String email;

    public Phase2User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Phase2User{id=" + id + ", userName='" + userName + "', email='" + email + "'}";
    }
}
