package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.service.PasswordResetService;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email kiritilmagan"));
        passwordResetService.sendResetCode(email);
        return ResponseEntity.ok(Map.of("message", email + " ga tasdiqlash kodi yuborildi"));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, String>> verifyCode(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code  = body.get("code");
        passwordResetService.verifyResetCode(email, code);
        return ResponseEntity.ok(Map.of("message", "Kod tasdiqlandi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String code        = body.get("code");
        String newPassword = body.get("newPassword");
        passwordResetService.resetPassword(email, code, newPassword);
        return ResponseEntity.ok(Map.of("message", "Parol muvaffaqiyatli o'zgartirildi"));
    }
}