package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.TestResult;
import uz.eduplatform.entity.Question;
import java.util.List;
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByUserId(Long userId);
    List<TestResult> findAllByOrderByCompletedAtDesc();
}
