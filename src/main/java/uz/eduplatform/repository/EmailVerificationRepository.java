package uz.eduplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.EmailVerification;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findByEmailAndCode(String email, String code);
    void deleteByEmail(String email);
}