package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    private boolean hasSchool(Authentication auth) {
        if (auth == null) return false;
        User user = (User) auth.getPrincipal();
        user = userRepository.findById(user.getId()).orElse(user);
        return user.getSchool() != null || user.getRole() == User.Role.ADMIN;
    }

    @GetMapping("/subjects")
    public ResponseEntity<?> getSubjects(Authentication auth) {
        if (!hasSchool(auth)) return ResponseEntity.status(403).body("Avval maktabingizni tanlang!");
        return ResponseEntity.ok(subjectRepository.findAll());
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<Subject> getSubject(@PathVariable Long id) {
        return subjectRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/subjects/{id}/chapters")
    public ResponseEntity<?> getChapters(@PathVariable Long id, Authentication auth) {
        if (!hasSchool(auth)) return ResponseEntity.status(403).body("Avval maktabingizni tanlang!");
        return ResponseEntity.ok(chapterRepository.findBySubjectIdOrderByOrderNum(id));
    }

    @GetMapping("/chapters/{id}/topics")
    public ResponseEntity<?> getTopics(@PathVariable Long id, Authentication auth) {
        if (!hasSchool(auth)) return ResponseEntity.status(403).body("Avval maktabingizni tanlang!");
        return ResponseEntity.ok(topicRepository.findByChapterIdOrderByOrderNum(id));
    }

    @GetMapping("/topics/{id}/questions")
    public ResponseEntity<?> getTopicQuestions(@PathVariable Long id, Authentication auth) {
        if (!hasSchool(auth)) return ResponseEntity.status(403).body("Avval maktabingizni tanlang!");
        List<Question> questions = questionRepository.findByTopicId(id);
        List<QuestionDto> dtos = questions.stream().map(QuestionDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/chapters/{id}/questions")
    public ResponseEntity<?> getChapterQuestions(@PathVariable Long id, Authentication auth) {
        if (!hasSchool(auth)) return ResponseEntity.status(403).body("Avval maktabingizni tanlang!");
        List<Question> questions = questionRepository.findByChapterId(id);
        List<QuestionDto> dtos = questions.stream().map(QuestionDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    static class QuestionDto {
        public Long id;
        public String questionText;
        public List<String> options;
        public int correctOptionIndex = -1;

        public QuestionDto(Question q) {
            this.id = q.getId();
            this.questionText = q.getQuestionText();
            this.options = q.getOptions();
        }
    }
}