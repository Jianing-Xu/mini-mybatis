package com.xujn.minimybatis.examples.phase3;

/**
 * Example result type for phase 3 statement reuse scenarios.
 *
 * <p>Responsibility: provide a stable JavaBean target so the phase 3 example
 * focuses on executor behavior instead of new mapping rules.
 *
 * <p>Thread-safety: mutable and not thread-safe.
 */
public class Phase3User {

    private Long id;
    private String username;
    private String email;

    public Phase3User() {
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
        return "Phase3User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}
