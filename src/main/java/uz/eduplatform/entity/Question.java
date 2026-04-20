package uz.eduplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity @Table(name = "questions") @Data @NoArgsConstructor
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options;
    private int correctOptionIndex;
    @Enumerated(EnumType.STRING)
    private QuestionType type;
    @ManyToOne @JoinColumn(name = "topic_id")
    @JsonIgnore
    private Topic topic;
    @ManyToOne @JoinColumn(name = "chapter_id")
    @JsonIgnore
    private Chapter chapter;
    public enum QuestionType { TOPIC, CHAPTER }
}
