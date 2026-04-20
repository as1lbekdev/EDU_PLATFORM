package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import uz.eduplatform.service.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TestService testService;
    private final ExcelService excelService;

    @PostMapping("/subjects")
    public ResponseEntity<Subject> createSubject(@RequestBody Subject s) {
        return ResponseEntity.ok(subjectRepository.save(s));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<Subject> updateSubject(@PathVariable Long id, @RequestBody Subject u) {
        return subjectRepository.findById(id).map(s -> {
            s.setName(u.getName());
            s.setDescription(u.getDescription());
            s.setIcon(u.getIcon());
            return ResponseEntity.ok(subjectRepository.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/chapters")
    public ResponseEntity<Chapter> createChapter(@RequestBody Map<String, Object> body) {
        Chapter c = new Chapter();
        c.setName((String) body.get("name"));
        c.setOrderNum((Integer) body.getOrDefault("orderNum", 0));
        c.setTestDurationMinutes((Integer) body.getOrDefault("testDurationMinutes", 60));
        c.setSubject(subjectRepository.getReferenceById(Long.valueOf(body.get("subjectId").toString())));
        return ResponseEntity.ok(chapterRepository.save(c));
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        chapterRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/topics")
    public ResponseEntity<Topic> createTopic(@RequestBody Map<String, Object> body) {
        Topic t = new Topic();
        t.setName((String) body.get("name"));
        t.setOrderNum((Integer) body.getOrDefault("orderNum", 0));
        t.setVideoUrl((String) body.getOrDefault("videoUrl", ""));
        t.setTestDurationMinutes((Integer) body.getOrDefault("testDurationMinutes", 30));
        t.setChapter(chapterRepository.getReferenceById(Long.valueOf(body.get("chapterId").toString())));
        return ResponseEntity.ok(topicRepository.save(t));
    }

    @PutMapping("/topics/{id}")
    public ResponseEntity<Topic> updateTopic(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return topicRepository.findById(id).map(t -> {
            if (body.containsKey("name")) t.setName((String) body.get("name"));
            if (body.containsKey("videoUrl")) t.setVideoUrl((String) body.get("videoUrl"));
            if (body.containsKey("testDurationMinutes"))
                t.setTestDurationMinutes((Integer) body.get("testDurationMinutes"));
            return ResponseEntity.ok(topicRepository.save(t));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import/topic-questions/{topicId}")
    public ResponseEntity<Map<String, Object>> importTopic(@PathVariable Long topicId, @RequestParam("file") MultipartFile file) throws Exception {
        int count = excelService.importTopicQuestions(file, topicId);
        return ResponseEntity.ok(Map.of("imported", count, "message", count + " ta savol qo'shildi"));
    }

    @PostMapping("/import/chapter-questions/{chapterId}")
    public ResponseEntity<Map<String, Object>> importChapter(@PathVariable Long chapterId, @RequestParam("file") MultipartFile file) throws Exception {
        int count = excelService.importChapterQuestions(file, chapterId);
        return ResponseEntity.ok(Map.of("imported", count, "message", count + " ta savol qo'shildi"));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> users() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/results/masked")
    public ResponseEntity<List<Map<String, Object>>> maskedResults() {
        List<Map<String, Object>> list = new ArrayList<>();
        testService.getAllResults().forEach(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            String email = r.getUser().getEmail();
            String masked = email.length() >= 2 ? email.substring(0, 2) + "**@" + email.split("@")[1] : email;
            m.put("id", r.getId());
            m.put("email", masked);
            m.put("fullName", r.getUser().getFullName());
            m.put("percentage", r.getPercentage());
            m.put("passed", r.isPassed());
            m.put("correctAnswers", r.getCorrectAnswers());
            m.put("totalQuestions", r.getTotalQuestions());
            m.put("testType", r.getTestType());
            m.put("completedAt", r.getCompletedAt());
            list.add(m);
        });
        return ResponseEntity.ok(list);
    }
}
