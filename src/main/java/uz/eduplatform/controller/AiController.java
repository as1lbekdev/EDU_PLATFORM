package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.entity.User;
import uz.eduplatform.service.AiService;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/result/{testResultId}")
    public ResponseEntity<Map<String, Object>> analyzeResult(@PathVariable Long testResultId) {
        return ResponseEntity.ok(aiService.analyzeTestResult(testResultId));
    }

    @GetMapping("/study-recommendations")
    public ResponseEntity<Map<String, Object>> studyRecommendations(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(aiService.getStudyRecommendations(user.getId()));
    }
}