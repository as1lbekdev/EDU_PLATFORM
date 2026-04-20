import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import uz.eduplatform.entity.*;
import uz.eduplatform.service.EmailService;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - Funksional testlar")
class EmailServiceFunctionalTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private EmailService emailService;

    private User makeUser() {
        User u = new User();
        u.setEmail("student@gmail.com");
        u.setFullName("Irgashaliyev Asilbek");
        return u;
    }

    private TestResult makeResult(int correct, int total, boolean passed) {
        TestResult r = new TestResult();
        r.setCorrectAnswers(correct);
        r.setTotalQuestions(total);
        r.setPercentage(total > 0 ? correct * 100.0 / total : 0);
        r.setPassed(passed);
        r.setTimeTakenSeconds(120);
        return r;
    }


    @Test
    @DisplayName("1.1 - Tasdiqlash kodi emaili yuboriladi")
    void sendVerificationCode_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendVerificationCode("test@gmail.com", "123456")
        );

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("1.2 - Email yuborishda xatolik bo'lsa RuntimeException otiladi")
    void sendVerificationCode_mailError_throwsException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP xatosi"));

        assertThrows(RuntimeException.class, () ->
                emailService.sendVerificationCode("test@gmail.com", "123456")
        );
    }


    @Test
    @DisplayName("2.1 - Mavzu test natijasi emaili yuboriladi")
    void sendTopicTestResult_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(mimeMessage);

        User user = makeUser();
        TestResult result = makeResult(8, 10, true);

        assertDoesNotThrow(() ->
                emailService.sendTopicTestResult(user, result, "1-mavzu: Algebra", "Matematika")
        );

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("2.2 - O'tmadi holatida ham mavzu test emaili yuboriladi")
    void sendTopicTestResult_failedResult_emailStillSent() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(mimeMessage);

        User user = makeUser();
        TestResult result = makeResult(3, 10, false);

        assertDoesNotThrow(() ->
                emailService.sendTopicTestResult(user, result, "2-mavzu", "Fizika")
        );

        verify(mailSender, times(1)).send(mimeMessage);
    }


    @Test
    @DisplayName("3.1 - Bob test natijasi emaili yuboriladi")
    void sendChapterTestResult_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(mimeMessage);

        User user = makeUser();
        TestResult result = makeResult(15, 20, true);

        assertDoesNotThrow(() ->
                emailService.sendChapterTestResult(user, result, "1-bob", "Matematika")
        );

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("3.2 - Bob test email xatolikda exception otilmaydi (try-catch)")
    void sendChapterTestResult_mailError_noException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP xatosi"));

        User user = makeUser();
        TestResult result = makeResult(5, 20, false);

        // sendChapterTestResult ichida try-catch bor, exception otilmasligi kerak
        assertDoesNotThrow(() ->
                emailService.sendChapterTestResult(user, result, "2-bob", "Fizika")
        );
    }
}