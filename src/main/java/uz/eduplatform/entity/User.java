package uz.eduplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Data @NoArgsConstructor
@ToString(exclude = "school")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    private String fullName;
    @Enumerated(EnumType.STRING)
    private Role role = Role.STUDENT;
    private boolean enabled = true;
    private String avatarUrl;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "school_id")
    private School school;
    private int schoolChangeCount = 0;

    public boolean canChangeSchool() {
        return schoolChangeCount < 3;
    }

    public enum Role { STUDENT, ADMIN }
}