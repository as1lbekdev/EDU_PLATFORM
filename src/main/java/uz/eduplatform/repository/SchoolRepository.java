package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.School;
import java.util.List;
public interface SchoolRepository extends JpaRepository<School, Long> {
    List<School> findByDistrictId(Long districtId);
}
