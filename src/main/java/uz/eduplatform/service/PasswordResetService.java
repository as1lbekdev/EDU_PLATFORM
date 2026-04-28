package uz.eduplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.entity.EmailVerification;
import uz.eduplatform.repository.EmailVerificationRepository;
import uz.eduplatform.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository verificationRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendResetCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("Bu email bilan ro'yxatdan o'tilmagan");
        }

        verificationRepository.deleteByEmail(email);

        String code = String.format("%06d", new Random().nextInt(999999));

        EmailVerification v = new EmailVerification();
        v.setEmail(email);
        v.setCode(code);
        v.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationRepository.save(v);

        emailService.sendPasswordResetCode(email, code);
    }

    public void verifyResetCode(String email, String code) {
        EmailVerification v = verificationRepository
                .findByEmailAndCode(email, code)
                .orElseThrow(() -> new RuntimeException("Kod noto'g'ri"));

        if (v.isExpired()) {
            verificationRepository.delete(v);
            throw new RuntimeException("Kod muddati tugagan. Qayta yuborish kerak");
        }
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Parol kamida 6 ta belgi bo'lishi kerak");
        }

        EmailVerification v = verificationRepository
                .findByEmailAndCode(email, code)
                .orElseThrow(() -> new RuntimeException("Kod noto'g'ri yoki muddati tugagan"));

        if (v.isExpired()) {
            verificationRepository.delete(v);
            throw new RuntimeException("Kod muddati tugagan. Qayta boshlash kerak");
        }

        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);
                },
                () -> { throw new RuntimeException("Foydalanuvchi topilmadi"); }
        );


        verificationRepository.delete(v);
    }
}