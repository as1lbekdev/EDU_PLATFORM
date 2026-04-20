package uz.eduplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.eduplatform.dto.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final QuestionRepository questionRepository;
    private final TestResultRepository testResultRepository;
    private final TopicRepository topicRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public TestResultDto submitTopicTest(Long userId, SubmitTestRequest req) {
        List<Question> questions = questionRepository.findFreshByTopicId(req.getTopicId());
        log.info("TOPIC TEST: topicId={}, savollar={}", req.getTopicId(), questions.size());
        TestResultDto dto = processTest(userId, questions, req, Question.QuestionType.TOPIC, req.getTopicId(), null);
        userRepository.findById(userId).ifPresent(user ->
                testResultRepository.findById(dto.getId()).ifPresent(tr ->
                        topicRepository.findById(req.getTopicId()).ifPresent(topic -> {
                            String subName = topic.getChapter() != null && topic.getChapter().getSubject() != null
                                    ? topic.getChapter().getSubject().getName() : "Fan";
                            emailService.sendTopicTestResult(user, tr, topic.getName(), subName);
                        })));
        return dto;
    }

    public TestResultDto submitChapterTest(Long userId, SubmitTestRequest req) {
        List<Question> questions = questionRepository.findFreshByChapterId(req.getChapterId());
        log.info("CHAPTER TEST: chapterId={}, savollar={}", req.getChapterId(), questions.size());
        TestResultDto dto = processTest(userId, questions, req, Question.QuestionType.CHAPTER, null, req.getChapterId());
        userRepository.findById(userId).ifPresent(user ->
                testResultRepository.findById(dto.getId()).ifPresent(tr ->
                        chapterRepository.findById(req.getChapterId()).ifPresent(ch -> {
                            String subName = ch.getSubject() != null ? ch.getSubject().getName() : "Fan";
                            emailService.sendChapterTestResult(user, tr, ch.getName(), subName);
                        })));
        return dto;
    }

    private TestResultDto processTest(Long userId, List<Question> questions, SubmitTestRequest req,
                                      Question.QuestionType type, Long topicId, Long chapterId) {
        int correct = 0;
        for (Question q : questions) {
            Integer ans = req.getAnswers().get(q.getId());
            log.info("  id={}, correctIdx={}, userAns={}", q.getId(), q.getCorrectOptionIndex(), ans);
            if (ans != null && ans == q.getCorrectOptionIndex()) correct++;
        }
        int total = questions.size();
        double pct = total > 0 ? (correct * 100.0 / total) : 0;
        boolean passed = pct >= 60.0;
        log.info("NATIJA: correct={}, total={}, pct={}", correct, total, pct);

        TestResult r = new TestResult();
        r.setUser(userRepository.getReferenceById(userId));
        r.setTotalQuestions(total);
        r.setCorrectAnswers(correct);
        r.setPercentage(pct);
        r.setPassed(passed);
        r.setTestType(type);
        r.setTimeTakenSeconds(req.getTimeTakenSeconds());
        if (topicId != null) r.setTopic(topicRepository.getReferenceById(topicId));
        if (chapterId != null) r.setChapter(chapterRepository.getReferenceById(chapterId));
        try { r.setAnswersJson(new ObjectMapper().writeValueAsString(req.getAnswers())); } catch (Exception ignored) {}
        TestResult saved = testResultRepository.save(r);

        TestResultDto dto = new TestResultDto();
        dto.setId(saved.getId());
        dto.setCorrectAnswers(correct);
        dto.setTotalQuestions(total);
        dto.setPercentage(pct);
        dto.setPassed(passed);
        dto.setCompletedAt(saved.getCompletedAt().toString());
        return dto;
    }

    public List<TestResult> getUserResults(Long userId) {
        return testResultRepository.findByUserId(userId); }
    public List<TestResult> getAllResults() { return testResultRepository.findAllByOrderByCompletedAtDesc(); }
}