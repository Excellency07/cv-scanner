package com.backend.cv_scanner.dao.entity;

import com.backend.cv_scanner.model.enums.AccountRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 character long")
    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Size(min =6, message = "Password must contains at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$",
            message = "Password must contain at least one uppercase and one lowercase letters, one number and one symbol")
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private List<AccountRole> roles = new ArrayList<>();

    public Account(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void addRole(AccountRole role) {
        if (role != null) {
            roles.add(role);
        }
    }

    public void removeRole(AccountRole role) {
        if (role != null) {
            roles.remove(role);
        }
    }
}
