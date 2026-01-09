package com.ecommerce.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;


    private String password;
    private String email;
    @Enumerated(EnumType.STRING)
    private Role role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Explicit setter accepting the Role enum (replaces Lombok-generated setter for clarity)
    public void setRole(Role role) {
        this.role = role;
    }

    // Convenience setter that accepts common string values like "USER" or "ROLE_USER"
    public void setRole(String roleStr) {
        if (roleStr == null) {
            this.role = null;
            return;
        }
        String normalized = roleStr.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            this.role = Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            // Unknown value - fallback to ROLE_USER (or you can throw an exception)
            this.role = Role.ROLE_USER;
        }
    }
    // Explicit getter for role (keeps symmetry with explicit setters and helps clarity)
    public Role getRole() {
        return this.role;
    }

}
