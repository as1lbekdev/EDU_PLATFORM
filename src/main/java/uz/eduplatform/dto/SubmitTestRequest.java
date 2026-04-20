package uz.eduplatform.dto;
import lombok.Data;
import java.util.Map;
@Data public class SubmitTestRequest {
    private Long topicId;
    private Long chapterId;
    private Map<Long, Integer> answers;
    private int timeTakenSeconds;
}
