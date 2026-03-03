package com.xujn.minimybatis.examples.phase2;

/**
 * Bean parameter model for phase 2 example queries.
 *
 * <p>Responsibility: verify that the parameter handler can read JavaBean
 * properties by name from one argument object.
 *
 * <p>Thread-safety: mutable and not thread-safe.
 */
public class Phase2UserFilter {

    private Long id;
    private String username;

    public Phase2UserFilter() {
    }

    public Phase2UserFilter(Long id, String username) {
        this.id = id;
        this.username = username;
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
}
