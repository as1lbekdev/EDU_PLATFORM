package uz.eduplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.eduplatform.entity.Question;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTopicId(Long topicId);
    List<Question> findByChapterId(Long chapterId);

    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId")
    List<Question> findFreshByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT q FROM Question q WHERE q.chapter.id = :chapterId")
    List<Question> findFreshByChapterId(@Param("chapterId") Long chapterId);
}