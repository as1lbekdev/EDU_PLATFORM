package uz.eduplatform.dto;
import lombok.Data;
@Data public class TestResultDto {
    private Long id;
    private int correctAnswers;
    private int totalQuestions;
    private double percentage;
    private boolean passed;
    private String completedAt;
}
