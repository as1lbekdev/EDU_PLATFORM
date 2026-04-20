package uz.eduplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.dto.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.service.TestService;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {
    private final TestService testService;

    @PostMapping("/topic/submit")
    public ResponseEntity<TestResultDto> submitTopic(@RequestBody SubmitTestRequest req, Authentication auth) {
        return ResponseEntity.ok(testService.submitTopicTest(getUser(auth).getId(), req));
    }

    @PostMapping("/chapter/submit")
    public ResponseEntity<TestResultDto> submitChapter(@RequestBody SubmitTestRequest req, Authentication auth) {
        return ResponseEntity.ok(testService.submitChapterTest(getUser(auth).getId(), req));
    }

    @GetMapping("/my-results")
    public ResponseEntity<List<TestResult>> myResults(Authentication auth) {
        return ResponseEntity.ok(testService.getUserResults(getUser(auth).getId()));
    }

    private User getUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}
