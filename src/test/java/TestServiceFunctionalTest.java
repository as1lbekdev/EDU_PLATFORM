

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.dto.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import uz.eduplatform.service.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestService - Funksional testlar")
class TestServiceFunctionalTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private TestResultRepository testResultRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks private TestService testService;

    private Question makeQuestion(Long id, int correctIdx) {
        Question q = new Question();
        q.setId(id);
        q.setQuestionText("Savol " + id);
        q.setCorrectOptionIndex(correctIdx);
        q.setOptions(List.of("A", "B", "C", "D"));
        q.setType(Question.QuestionType.TOPIC);
        return q;
    }

    private User makeUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("test@gmail.com");
        u.setFullName("Test Foydalanuvchi");
        return u;
    }


    @Test
    @DisplayName("1.1 - Barcha javob to'g'ri bo'lsa 100% natija")
    void topicTest_allCorrect_100percent() {
        List<Question> questions = List.of(
                makeQuestion(1L, 0),
                makeQuestion(2L, 1),
                makeQuestion(3L, 2)
        );

        Map<Long, Integer> answers = Map.of(1L, 0, 2L, 1, 3L, 2);
        SubmitTestRequest req = new SubmitTestRequest();
        req.setTopicId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(60);

        when(questionRepository.findFreshByTopicId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(makeUser(1L));
        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
        when(testResultRepository.findById(1L)).thenReturn(Optional.of(new TestResult()));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(new Topic()));

        TestResultDto result = testService.submitTopicTest(1L, req);

        assertEquals(3, result.getCorrectAnswers());
        assertEquals(3, result.getTotalQuestions());
        assertEquals(100.0, result.getPercentage(), 0.01);
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("1.2 - Hech qaysi javob to'g'ri bo'lmasa 0% va o'tmadi")
    void topicTest_allWrong_0percent_failed() {
        List<Question> questions = List.of(
                makeQuestion(1L, 0),
                makeQuestion(2L, 1),
                makeQuestion(3L, 2)
        );

        Map<Long, Integer> answers = Map.of(1L, 3, 2L, 3, 3L, 3);
        SubmitTestRequest req = new SubmitTestRequest();
        req.setTopicId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(120);

        when(questionRepository.findFreshByTopicId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(makeUser(1L));
        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0);
            r.setId(2L);
            return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
        when(testResultRepository.findById(2L)).thenReturn(Optional.of(new TestResult()));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(new Topic()));

        TestResultDto result = testService.submitTopicTest(1L, req);

        assertEquals(0, result.getCorrectAnswers());
        assertEquals(0.0, result.getPercentage(), 0.01);
        assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("1.3 - 60% chegara: 6/10 = o'tdi")
    void topicTest_60percent_passed() {
        List<Question> questions = new ArrayList<>();
        for (long i = 1; i <= 10; i++) questions.add(makeQuestion(i, 0));

        Map<Long, Integer> answers = new HashMap<>();
        for (long i = 1; i <= 6; i++) answers.put(i, 0);
        for (long i = 7; i <= 10; i++) answers.put(i, 1);

        SubmitTestRequest req = new SubmitTestRequest();
        req.setTopicId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(300);

        when(questionRepository.findFreshByTopicId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(makeUser(1L));
        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0); r.setId(3L); return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
        when(testResultRepository.findById(3L)).thenReturn(Optional.of(new TestResult()));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(new Topic()));

        TestResultDto result = testService.submitTopicTest(1L, req);

        assertEquals(6, result.getCorrectAnswers());
        assertEquals(60.0, result.getPercentage(), 0.01);
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("1.4 - 59% chegara: 5/10 = o'tmadi")
    void topicTest_59percent_failed() {
        List<Question> questions = new ArrayList<>();
        for (long i = 1; i <= 10; i++) questions.add(makeQuestion(i, 0));

        Map<Long, Integer> answers = new HashMap<>();
        for (long i = 1; i <= 5; i++) answers.put(i, 0);
        for (long i = 6; i <= 10; i++) answers.put(i, 1);

        SubmitTestRequest req = new SubmitTestRequest();
        req.setTopicId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(200);

        when(questionRepository.findFreshByTopicId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(makeUser(1L));
        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0); r.setId(4L); return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
        when(testResultRepository.findById(4L)).thenReturn(Optional.of(new TestResult()));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(new Topic()));

        TestResultDto result = testService.submitTopicTest(1L, req);

        assertEquals(5, result.getCorrectAnswers());
        assertEquals(50.0, result.getPercentage(), 0.01);
        assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("1.5 - Javob berilmagan savollar 0 sanaladi")
    void topicTest_unansweredQuestions_countedAsWrong() {
        List<Question> questions = List.of(
                makeQuestion(1L, 0),
                makeQuestion(2L, 1),
                makeQuestion(3L, 2)
        );

        Map<Long, Integer> answers = Map.of(1L, 0);
        SubmitTestRequest req = new SubmitTestRequest();
        req.setTopicId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(30);

        when(questionRepository.findFreshByTopicId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(makeUser(1L));
        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0); r.setId(5L); return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
        when(testResultRepository.findById(5L)).thenReturn(Optional.of(new TestResult()));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(new Topic()));

        TestResultDto result = testService.submitTopicTest(1L, req);

        assertEquals(1, result.getCorrectAnswers());
        assertEquals(3, result.getTotalQuestions());
    }


    @Test
    @DisplayName("2.1 - Bob testi muvaffaqiyatli topshiriladi")
    void chapterTest_success() {
        List<Question> questions = List.of(
                makeQuestion(1L, 0),
                makeQuestion(2L, 1)
        );
        questions.forEach(q -> q.setType(Question.QuestionType.CHAPTER));

        Map<Long, Integer> answers = Map.of(1L, 0, 2L, 1);
        SubmitTestRequest req = new SubmitTestRequest();
        req.setChapterId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(180);

        when(questionRepository.findFreshByChapterId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(makeUser(1L));
        when(chapterRepository.getReferenceById(1L)).thenReturn(new Chapter());
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0); r.setId(6L); return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
        when(testResultRepository.findById(6L)).thenReturn(Optional.of(new TestResult()));
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(new Chapter()));

        TestResultDto result = testService.submitChapterTest(1L, req);

        assertEquals(2, result.getCorrectAnswers());
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("2.2 - Test natijasi emailga yuboriladi")
    void chapterTest_emailSent() {
        List<Question> questions = List.of(makeQuestion(1L, 0));
        questions.get(0).setType(Question.QuestionType.CHAPTER);

        Map<Long, Integer> answers = Map.of(1L, 0);
        SubmitTestRequest req = new SubmitTestRequest();
        req.setChapterId(1L);
        req.setAnswers(answers);
        req.setTimeTakenSeconds(60);

        User user = makeUser(1L);
        Chapter chapter = new Chapter();
        chapter.setName("1-bob");
        Subject subject = new Subject();
        subject.setName("Matematika");
        chapter.setSubject(subject);

        when(questionRepository.findFreshByChapterId(1L)).thenReturn(questions);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(chapterRepository.getReferenceById(1L)).thenReturn(chapter);
        when(testResultRepository.save(any())).thenAnswer(i -> {
            TestResult r = i.getArgument(0); r.setId(7L); return r;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        TestResult savedResult = new TestResult();
        when(testResultRepository.findById(7L)).thenReturn(Optional.of(savedResult));
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        doNothing().when(emailService).sendChapterTestResult(any(), any(), anyString(), anyString());

        testService.submitChapterTest(1L, req);

        verify(emailService, times(1)).sendChapterTestResult(eq(user), any(), anyString(), anyString());
    }


    @Test
    @DisplayName("3.1 - Foydalanuvchi natijalari ro'yxati qaytariladi")
    void getUserResults_returnsList() {
        List<TestResult> results = List.of(new TestResult(), new TestResult());
        when(testResultRepository.findByUserId(1L)).thenReturn(results);

        List<TestResult> found = testService.getUserResults(1L);

        assertEquals(2, found.size());
        verify(testResultRepository).findByUserId(1L);
    }
}