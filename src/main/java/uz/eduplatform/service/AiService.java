package uz.eduplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final TestResultRepository testResultRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final ChapterRepository chapterRepository;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public Map<String, Object> analyzeTestResult(Long testResultId) {
        TestResult result = testResultRepository.findById(testResultId).orElse(null);
        if (result == null) return Map.of("feedback", "Natija topilmadi.");

        List<Question> questions;
        if (result.getTestType() == Question.QuestionType.TOPIC && result.getTopic() != null) {
            questions = questionRepository.findByTopicId(result.getTopic().getId());
        } else if (result.getChapter() != null) {
            questions = questionRepository.findByChapterId(result.getChapter().getId());
        } else {
            return Map.of("feedback", "Savollar topilmadi.");
        }

        Map<Long, Integer> userAnswers = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Integer> raw = mapper.readValue(result.getAnswersJson(), Map.class);
            raw.forEach((k, v) -> userAnswers.put(Long.parseLong(k), v));
        } catch (Exception e) {
            return Map.of("feedback", "Javoblar o'qilmadi.");
        }

        List<Map<String, Object>> wrongList = new ArrayList<>();
        for (Question q : questions) {
            List<String> options = q.getOptions();
            if (options == null || options.isEmpty()) continue;
            Integer userAns = userAnswers.get(q.getId());
            int correctIdx = q.getCorrectOptionIndex();
            boolean validCorrect = correctIdx >= 0 && correctIdx < options.size();
            boolean validUser = userAns != null && userAns >= 0 && userAns < options.size();
            String correctAnswer = validCorrect ? options.get(correctIdx) : "Noma'lum";
            String userAnswer = validUser ? options.get(userAns) : "Javob berilmagan";
            boolean isCorrect = validUser && userAns == correctIdx;
            if (!isCorrect) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("question", q.getQuestionText());
                item.put("correctAnswer", correctAnswer);
                item.put("userAnswer", userAnswer);
                wrongList.add(item);
            }
        }

        String testName = result.getTestType() == Question.QuestionType.TOPIC
                ? (result.getTopic() != null ? result.getTopic().getName() : "Mavzu testi")
                : (result.getChapter() != null ? result.getChapter().getName() : "Bob testi");

        StringBuilder prompt = new StringBuilder();
        prompt.append("O'quvchi \"").append(testName).append("\" testini topshirdi.\n");
        prompt.append("Natija: ").append(result.getCorrectAnswers()).append("/")
                .append(result.getTotalQuestions())
                .append(String.format(" (%.1f%%)\n", result.getPercentage()));
        prompt.append(result.isPassed() ? "Holat: O'TDI\n\n" : "Holat: O'TMADI\n\n");
        if (!wrongList.isEmpty()) {
            prompt.append("Xato savollar:\n");
            for (int i = 0; i < wrongList.size(); i++) {
                Map<String, Object> w = wrongList.get(i);
                prompt.append(i+1).append(". ").append(w.get("question")).append("\n");
                prompt.append("   Tanlagan: ").append(w.get("userAnswer")).append("\n");
                prompt.append("   To'g'ri: ").append(w.get("correctAnswer")).append("\n\n");
            }
        }
        prompt.append("O'zbek tilida yoz (oddiy til, yulduzcha ishlatma):\n");
        if (result.isPassed()) {
            prompt.append("1. Qisqa tabrik\n");
            prompt.append("2. Qaysi mavzularni qayta o'rgansin (aniq ko'rsating)\n");
        } else {
            prompt.append("1. Qaysi mavzularni o'rganishi kerak (aniq ko'rsating)\n");
            prompt.append("2. 2-3 ta amaliy maslahat\n");
        }

        try {
            String aiText = callGemini(prompt.toString());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("passed", result.isPassed());
            response.put("percentage", result.getPercentage());
            response.put("correct", result.getCorrectAnswers());
            response.put("total", result.getTotalQuestions());
            response.put("feedback", aiText);
            response.put("wrongCount", wrongList.size());
            response.put("wrongDetails", wrongList);
            return response;
        } catch (Exception e) {
            StringBuilder fallback = new StringBuilder();
            fallback.append(result.isPassed() ? "Tabriklaymiz!\n\n" : "Quyidagi mavzularni qayta o'rganing:\n\n");
            wrongList.forEach(w -> fallback
                    .append("• ").append(w.get("question")).append("\n")
                    .append("  To'g'ri: ").append(w.get("correctAnswer")).append("\n\n"));
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("passed", result.isPassed());
            response.put("percentage", result.getPercentage());
            response.put("correct", result.getCorrectAnswers());
            response.put("total", result.getTotalQuestions());
            response.put("feedback", fallback.toString());
            response.put("wrongCount", wrongList.size());
            response.put("wrongDetails", wrongList);
            return response;
        }
    }

    public Map<String, Object> getStudyRecommendations(Long userId) {
        List<TestResult> results = testResultRepository.findByUserId(userId);
        if (results.isEmpty()) {
            return Map.of("message", "Hali test topshirilmagan!", "weakTopics", List.of());
        }

        List<Map<String, Object>> weakTopics = new ArrayList<>();
        for (TestResult r : results) {
            if (r.getTestType() == Question.QuestionType.TOPIC && r.getTopic() != null) {
                if (!r.isPassed() || r.getPercentage() < 80) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("topicId", r.getTopic().getId());
                    t.put("topicName", r.getTopic().getName());
                    t.put("score", r.getPercentage());
                    t.put("passed", r.isPassed());
                    t.put("videoUrl", r.getTopic().getVideoUrl());
                    t.put("priority", r.isPassed() ? "O'rtacha" : "Muhim");
                    weakTopics.add(t);
                }
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("O'quvchining zaif mavzulari:\n");
        weakTopics.forEach(t -> prompt
                .append("- ").append(t.get("topicName"))
                .append(": ").append(String.format("%.1f%%", (Double)t.get("score")))
                .append(Boolean.TRUE.equals(t.get("passed")) ? " (o'tdi)\n" : " (o'tmadi)\n"));
        prompt.append("\nO'zbek tilida:\n1. Qaysi mavzularni ustuvor o'rgansin\n2. Har biri uchun 1 maslahat\n3. Motivatsion so'z\nOddiy til, yulduzcha ishlatma.");

        String aiAdvice = "Zaif mavzulardan boshlang, har kuni 1 ta mavzu o'rganing.";
        try { aiAdvice = callGemini(prompt.toString()); } catch (Exception ignored) {}

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("advice", aiAdvice);
        result.put("weakTopics", weakTopics);
        result.put("totalWeak", weakTopics.size());
        return result;
    }

    private String callGemini(String prompt) throws Exception {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> res = rt.exchange(GEMINI_URL + geminiApiKey, HttpMethod.POST, req, Map.class);
        List candidates = (List) res.getBody().get("candidates");
        Map content = (Map)((Map)candidates.get(0)).get("content");
        List parts = (List) content.get("parts");
        return ((String)((Map)parts.get(0)).get("text"))
                .replaceAll("\\*+","").replaceAll("#+ ","").trim();
    }
}