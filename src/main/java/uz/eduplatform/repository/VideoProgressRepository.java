package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.VideoProgress;
import java.util.Optional;
import java.util.List;
public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {
    Optional<VideoProgress> findByUserIdAndTopicId(Long userId, Long topicId);
    List<VideoProgress> findByUserId(Long userId);
}
