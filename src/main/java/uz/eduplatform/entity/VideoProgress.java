package uz.eduplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "video_progress") @Data @NoArgsConstructor
public class VideoProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    @ManyToOne @JoinColumn(name = "topic_id")
    private Topic topic;
    private boolean watched = false;
    private int watchedSeconds = 0;
    private LocalDateTime updatedAt = LocalDateTime.now();
}
