package com.xujn.minimybatis.examples.integration.phase1;

/**
 * Example domain object used by the mini-spring integration demo.
 *
 * <p>Responsibility: act as the target resultType for XML mapped queries.
 *
 * <p>Thread-safety: mutable demo object, not thread-safe.
 */
public class IntegrationUser {

    private Long id;
    private String username;
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "IntegrationUser{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}
