package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.Topic;
import java.util.List;
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByChapterIdOrderByOrderNum(Long chapterId);
}
