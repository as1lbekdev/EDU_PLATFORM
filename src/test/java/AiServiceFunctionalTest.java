

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import uz.eduplatform.service.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService - Funksional testlar")
class AiServiceFunctionalTest {

    @Mock private TestResultRepository testResultRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ChapterRepository chapterRepository;

    @InjectMocks private AiService aiService;

    private Question makeQuestion(Long id, int correctIdx, String text) {
        Question q = new Question();
        q.setId(id);
        q.setQuestionText(text);
        q.setCorrectOptionIndex(correctIdx);
        q.setOptions(List.of("A variant", "B variant", "C variant", "D variant"));
        return q;
    }

    @Test
    @DisplayName("1.1 - Natija topilmasa 'topilmadi' xabari qaytariladi")
    void analyzeTestResult_notFound_returnsMessage() {
        when(testResultRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Object> result = aiService.analyzeTestResult(999L);

        assertEquals("Natija topilmadi.", result.get("feedback"));
    }

    @Test
    @DisplayName("1.2 - Natija mavjud bo'lsa feedback qaytariladi")
    void analyzeTestResult_existing_returnsFeedback() {
        TestResult tr = new TestResult();
        tr.setId(1L);
        tr.setCorrectAnswers(7);
        tr.setTotalQuestions(10);
        tr.setPercentage(70.0);
        tr.setPassed(true);
        tr.setTestType(Question.QuestionType.TOPIC);
        tr.setAnswersJson("{\"1\":0,\"2\":1,\"3\":2}");

        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("Algebra");
        tr.setTopic(topic);

        Question q1 = makeQuestion(1L, 0, "Savol 1");
        Question q2 = makeQuestion(2L, 1, "Savol 2");
        Question q3 = makeQuestion(3L, 0, "Savol 3"); // 3 noto'g'ri

        when(testResultRepository.findById(1L)).thenReturn(Optional.of(tr));
        when(questionRepository.findFreshByTopicId(1L)).thenReturn(List.of(q1, q2, q3));

        Map<String, Object> result = aiService.analyzeTestResult(1L);

        assertNotNull(result.get("feedback"));
        assertNotNull(result.get("wrongCount"));
        assertNotNull(result.get("wrongDetails"));

        // To'g'ri javob ko'rsatilmasligi kerak
        List<Map<String, Object>> wrongDetails = (List<Map<String, Object>>) result.get("wrongDetails");
        for (Map<String, Object> item : wrongDetails) {
            assertFalse(item.containsKey("correctAnswer"),
                    "To'g'ri javob wrongDetails da bo'lmasligi kerak!");
        }
    }

    @Test
    @DisplayName("1.3 - O'quvchi javobida faqat userAnswer ko'rsatiladi")
    void analyzeTestResult_wrongDetails_onlyUserAnswer() {
        TestResult tr = new TestResult();
        tr.setId(2L);
        tr.setCorrectAnswers(0);
        tr.setTotalQuestions(2);
        tr.setPercentage(0.0);
        tr.setPassed(false);
        tr.setTestType(Question.QuestionType.TOPIC);
        tr.setAnswersJson("{\"1\":3,\"2\":3}"); // ikkalasi ham noto'g'ri

        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("Test mavzu");
        tr.setTopic(topic);

        when(testResultRepository.findById(2L)).thenReturn(Optional.of(tr));
        when(questionRepository.findFreshByTopicId(1L)).thenReturn(List.of(
                makeQuestion(1L, 0, "Savol 1"),
                makeQuestion(2L, 1, "Savol 2")
        ));

        Map<String, Object> result = aiService.analyzeTestResult(2L);
        List<Map<String, Object>> wrongDetails = (List<Map<String, Object>>) result.get("wrongDetails");

        assertEquals(2, wrongDetails.size());
        for (Map<String, Object> item : wrongDetails) {
            assertTrue(item.containsKey("question"));
            assertTrue(item.containsKey("userAnswer"));
            assertFalse(item.containsKey("correctAnswer")); // MUHIM: to'g'ri javob YO'Q
        }
    }
}
