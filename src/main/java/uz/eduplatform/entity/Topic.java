package uz.eduplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity @Table(name = "topics") @Data @NoArgsConstructor
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private int orderNum;
    private String videoUrl;
    private int testDurationMinutes = 30;
    @ManyToOne @JoinColumn(name = "chapter_id")
    @JsonIgnore
    private Chapter chapter;
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Question> questions;
}
