package uz.eduplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "test_results") @Data @NoArgsConstructor
public class TestResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    @ManyToOne @JoinColumn(name = "topic_id")
    private Topic topic;
    @ManyToOne @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    private int totalQuestions;
    private int correctAnswers;
    private double percentage;
    private boolean passed;
    private int timeTakenSeconds;
    @Enumerated(EnumType.STRING)
    private Question.QuestionType testType;
    @Column(columnDefinition = "TEXT")
    private String answersJson;
    private LocalDateTime completedAt = LocalDateTime.now();
}
