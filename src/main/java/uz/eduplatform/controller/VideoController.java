package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {
    private final VideoProgressRepository videoProgressRepository;
    private final TopicRepository topicRepository;

    @PostMapping("/progress/{topicId}")
    public ResponseEntity<Void> update(@PathVariable Long topicId, @RequestBody Map<String, Object> body, Authentication auth) {
        User user = (User) auth.getPrincipal();
        VideoProgress p = videoProgressRepository.findByUserIdAndTopicId(user.getId(), topicId).orElse(new VideoProgress());
        p.setUser(user);
        p.setTopic(topicRepository.getReferenceById(topicId));
        p.setWatched((Boolean) body.getOrDefault("watched", false));
        p.setWatchedSeconds((Integer) body.getOrDefault("watchedSeconds", 0));
        p.setUpdatedAt(LocalDateTime.now());
        videoProgressRepository.save(p);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/progress/{topicId}")
    public ResponseEntity<VideoProgress> get(@PathVariable Long topicId, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return videoProgressRepository.findByUserIdAndTopicId(user.getId(), topicId)
                .map(ResponseEntity::ok).orElse(ResponseEntity.ok(new VideoProgress()));
    }
}
