package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.entity.User;
import uz.eduplatform.repository.UserRepository;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg","image/png","image/jpg","image/webp");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        User user = (User) auth.getPrincipal();
        user = userRepository.findById(user.getId()).orElse(user);
        return ResponseEntity.ok(toMap(user));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Fayl bo'sh"));
        if (file.getSize() > MAX_SIZE) return ResponseEntity.badRequest().body(Map.of("error", "Fayl 5MB dan katta"));
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            return ResponseEntity.badRequest().body(Map.of("error", "Faqat JPG, PNG, WEBP ruxsat etiladi"));

        User user = (User) auth.getPrincipal();
        user = userRepository.findById(user.getId()).orElse(user);

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        if (user.getAvatarUrl() != null) {
            try {
                String oldFile = user.getAvatarUrl().replace("/api/profile/avatar/", "");
                Files.deleteIfExists(Paths.get(UPLOAD_DIR + oldFile));
            } catch (Exception ignored) {}
        }

        String ext = getExt(file.getOriginalFilename());
        String fileName = "user_" + user.getId() + "_" + System.currentTimeMillis() + ext;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        user.setAvatarUrl("/api/profile/avatar/" + fileName);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "avatarUrl", user.getAvatarUrl(),
                "message", "Rasm muvaffaqiyatli yuklandi"
        ));
    }

    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        if (!Files.exists(filePath)) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(filePath);
        String contentType = fileName.endsWith(".png") ? "image/png" :
                fileName.endsWith(".webp") ? "image/webp" : "image/jpeg";
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Cache-Control", "max-age=3600")
                .body(data);
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, Object>> deleteAvatar(Authentication auth) throws IOException {
        User user = (User) auth.getPrincipal();
        user = userRepository.findById(user.getId()).orElse(user);
        if (user.getAvatarUrl() != null) {
            String fileName = user.getAvatarUrl().replace("/api/profile/avatar/", "");
            Files.deleteIfExists(Paths.get(UPLOAD_DIR + fileName));
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
        return ResponseEntity.ok(Map.of("message", "Rasm o'chirildi"));
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("fullName", user.getFullName());
        m.put("role", user.getRole());
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().format(FMT) : "-");
        m.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().format(FMT) : "Birinchi kirish");
        return m;
    }

    private String getExt(String name) {
        if (name == null) return ".jpg";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : ".jpg";
    }
}