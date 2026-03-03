package com.xujn.minimybatis.examples.phase1;

/**
 * Example result type used by phase 1 query scenarios.
 *
 * <p>Responsibility: provide a simple JavaBean with field names aligned to SQL
 * column labels so phase 1 result mapping stays intentionally minimal.
 *
 * <p>Thread-safety: mutable and not thread-safe.
 */
public class User {

    private Long id;
    private String username;
    private String email;

    public User() {
    }

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
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}
