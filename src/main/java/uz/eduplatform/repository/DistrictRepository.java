package uz.eduplatform.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.entity.District;
import java.util.List;
public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByRegionId(Long regionId);
}
