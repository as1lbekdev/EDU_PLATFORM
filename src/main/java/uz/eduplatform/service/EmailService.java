package uz.eduplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uz.eduplatform.entity.TestResult;
import uz.eduplatform.entity.User;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ===== 1. Email tasdiqlash kodi =====
    public void sendVerificationCode(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("EduPlatform - Email tasdiqlash kodi");
            String html = String.format("""
                <div style='font-family:Arial;max-width:500px;margin:auto;padding:30px;
                            border-radius:16px;border:1px solid #e2e8f0'>
                    <div style='text-align:center;margin-bottom:24px'>
                        <h1 style='color:#2563eb;font-size:28px;margin:0'>📚 EduPlatform</h1>
                        <p style='color:#64748b;margin-top:8px'>Maktab o'quvchilari uchun online ta'lim</p>
                    </div>
                    <h2 style='color:#0f172a;font-size:20px'>Email manzilingizni tasdiqlang</h2>
                    <p style='color:#475569;line-height:1.6'>
                        Ro'yxatdan o'tishni yakunlash uchun quyidagi kodni kiriting:
                    </p>
                    <div style='background:#eff6ff;border-radius:12px;padding:24px;
                                text-align:center;margin:20px 0'>
                        <div style='font-size:40px;font-weight:800;letter-spacing:12px;
                                    color:#2563eb;font-family:monospace'>%s</div>
                    </div>
                    <p style='color:#94a3b8;font-size:13px'>
                        ⏱️ Bu kod <b>10 daqiqa</b> davomida amal qiladi.<br>
                        Agar siz ro'yxatdan o'tmagan bo'lsangiz, bu xabarni e'tiborsiz qoldiring.
                    </p>
                </div>
                """, code);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Tasdiqlash kodi yuborishda xatolik: " + e.getMessage());
            throw new RuntimeException("Email yuborib bo'lmadi: " + e.getMessage());
        }
    }

    // ===== 2. Mavzu testi natijasi =====
    public void sendTopicTestResult(User user, TestResult result, String topicName, String subjectName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("EduPlatform - Mavzu testi natijasi: " + topicName);

            boolean passed = result.isPassed();
            String statusColor = passed ? "#16a34a" : "#dc2626";
            String statusText = passed ? "✅ O'tdi" : "❌ O'tmadi";
            String message2 = passed
                    ? "Tabriklaymiz! Mavzu testidan muvaffaqiyatli o'tdingiz."
                    : "Afsuski, 60% dan past natija. Mavzuni qayta o'rganib, urinib ko'ring.";

            String html = buildResultEmail(user.getFullName(), subjectName, topicName,
                    "Mavzu testi", result, statusColor, statusText, message2);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Mavzu testi email xatolik: " + e.getMessage());
        }
    }

    // ===== 3. Bob testi natijasi =====
    public void sendChapterTestResult(User user, TestResult result, String chapterName, String subjectName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("EduPlatform - Bob testi natijasi: " + chapterName);

            boolean passed = result.isPassed();
            String statusColor = passed ? "#16a34a" : "#dc2626";
            String statusText = passed ? "✅ O'tdi" : "❌ O'tmadi";
            String message2 = passed
                    ? "Tabriklaymiz! Bob testidan o'tdingiz. Keyingi bobga o'tishingiz mumkin."
                    : "60% dan past natija. Bobni qayta o'rganib, sinab ko'ring.";

            String html = buildResultEmail(user.getFullName(), subjectName, chapterName,
                    "Bob testi", result, statusColor, statusText, message2);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Bob testi email xatolik: " + e.getMessage());
        }
    }

    private String buildResultEmail(String fullName, String subjectName, String testName,
                                    String testType, TestResult result,
                                    String statusColor, String statusText, String message) {
        return String.format("""
            <div style='font-family:Arial;max-width:600px;margin:auto;padding:30px;
                        border-radius:16px;border:1px solid #e2e8f0'>
                <div style='text-align:center;margin-bottom:24px'>
                    <h1 style='color:#2563eb;font-size:24px;margin:0'>📚 EduPlatform</h1>
                </div>
                <h2 style='color:#0f172a'>Test natijasi</h2>
                <p>Hurmatli <b>%s</b>,</p>
                <p><b>%s</b> fanidan <b>%s</b> bo'yicha <b>%s</b>:</p>
                <div style='background:#f0f9ff;border-radius:12px;padding:20px;margin:16px 0'>
                    <table style='width:100%%'>
                        <tr>
                            <td style='color:#64748b;padding:6px 0'>✔️ To'g'ri javoblar:</td>
                            <td style='font-weight:700;text-align:right'>%d / %d</td>
                        </tr>
                        <tr>
                            <td style='color:#64748b;padding:6px 0'>📈 Foiz:</td>
                            <td style='font-weight:700;text-align:right'>%.1f%%</td>
                        </tr>
                        <tr>
                            <td style='color:#64748b;padding:6px 0'>⏱️ Vaqt:</td>
                            <td style='font-weight:700;text-align:right'>%d daqiqa %d soniya</td>
                        </tr>
                    </table>
                </div>
                <div style='text-align:center;padding:16px;background:%s22;
                            border-radius:10px;border:2px solid %s;margin:16px 0'>
                    <span style='font-size:24px;font-weight:800;color:%s'>%s</span>
                </div>
                <p style='color:#475569;line-height:1.6'>%s</p>
                <div style='margin-top:24px;padding-top:16px;border-top:1px solid #e2e8f0;
                            color:#94a3b8;font-size:12px;text-align:center'>
                    EduPlatform — Maktab o'quvchilari uchun online ta'lim platformasi
                </div>
            </div>
            """,
                fullName, subjectName, testName, testType,
                result.getCorrectAnswers(), result.getTotalQuestions(),
                result.getPercentage(),
                result.getTimeTakenSeconds() / 60, result.getTimeTakenSeconds() % 60,
                statusColor, statusColor, statusColor, statusText,
                message);
    }
}