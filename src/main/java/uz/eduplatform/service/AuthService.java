package uz.eduplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.dto.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import uz.eduplatform.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationRepository verificationRepository;
    private final EmailService emailService;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email))
            throw new RuntimeException("Bu email allaqachon ro'yxatdan o'tgan");
        verificationRepository.deleteByEmail(email);
        String code = String.format("%06d", new Random().nextInt(999999));
        EmailVerification v = new EmailVerification();
        v.setEmail(email); v.setCode(code);
        v.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationRepository.save(v);
        emailService.sendVerificationCode(email, code);
    }

    @Transactional
    public AuthResponse verifyAndRegister(RegisterRequest req) {
        EmailVerification v = verificationRepository
                .findByEmailAndCode(req.getEmail(), req.getCode())
                .orElseThrow(() -> new RuntimeException("Kod noto'g'ri"));
        if (v.isExpired()) {
            verificationRepository.delete(v);
            throw new RuntimeException("Kod muddati tugagan. Qayta yuborish kerak");
        }
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Bu email allaqachon ro'yxatdan o'tgan");
        User user = new User();
        user.setEmail(req.getEmail());
        user.setFullName(req.getFullName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        verificationRepository.delete(v);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }

    @Transactional
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Email yoki parol noto'g'ri"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Email yoki parol noto'g'ri");
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }
}