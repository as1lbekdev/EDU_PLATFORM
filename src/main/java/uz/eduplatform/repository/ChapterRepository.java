package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.Chapter;
import java.util.List;
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdOrderByOrderNum(Long subjectId);
}
