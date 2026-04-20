package uz.eduplatform.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "email_verifications")
@Data @NoArgsConstructor
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String code; // 6 raqamli kod

    private boolean verified = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt; // 10 daqiqa

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
