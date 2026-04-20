package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.Subject;
public interface SubjectRepository extends JpaRepository<Subject, Long> {}
