

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.dto.*;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import uz.eduplatform.security.JwtUtil;
import uz.eduplatform.service.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Funksional testlar")
class AuthServiceFunctionalTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationRepository verificationRepository;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;


    @Test
    @DisplayName("1.1 - Yangi emailga kod yuboriladi")
    void sendCode_newEmail_success() {
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(verificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(anyString(), anyString());

        assertDoesNotThrow(() -> authService.sendVerificationCode("test@gmail.com"));

        verify(emailService, times(1)).sendVerificationCode(eq("test@gmail.com"), anyString());
        verify(verificationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("1.2 - Allaqachon ro'yxatdan o'tgan emailga kod yuborilmaydi")
    void sendCode_existingEmail_throwsException() {
        when(userRepository.existsByEmail("exist@gmail.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.sendVerificationCode("exist@gmail.com"));

        assertTrue(ex.getMessage().contains("allaqachon ro'yxatdan o'tgan"));
        verify(emailService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    @DisplayName("1.3 - Eski kod o'chiriladi, yangi kod saqlanadi")
    void sendCode_deletesOldCode_savesNewCode() {
        when(userRepository.existsByEmail("user@gmail.com")).thenReturn(false);
        doNothing().when(verificationRepository).deleteByEmail("user@gmail.com");
        when(verificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.sendVerificationCode("user@gmail.com");

        verify(verificationRepository).deleteByEmail("user@gmail.com");
        verify(verificationRepository).save(argThat(v ->
                v instanceof EmailVerification &&
                        ((EmailVerification) v).getEmail().equals("user@gmail.com") &&
                        ((EmailVerification) v).getCode() != null &&
                        ((EmailVerification) v).getCode().length() == 6
        ));
    }


    @Test
    @DisplayName("2.1 - To'g'ri kod bilan ro'yxatdan o'tish muvaffaqiyatli")
    void register_validCode_success() {
        EmailVerification v = new EmailVerification();
        v.setEmail("new@gmail.com");
        v.setCode("123456");
        v.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(verificationRepository.findByEmailAndCode("new@gmail.com", "123456"))
                .thenReturn(Optional.of(v));
        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwt-token");

        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@gmail.com");
        req.setCode("123456");
        req.setFullName("Test User");
        req.setPassword("pass123");

        AuthResponse res = authService.verifyAndRegister(req);

        assertNotNull(res);
        assertEquals("jwt-token", res.getToken());
        assertEquals("new@gmail.com", res.getEmail());
        verify(verificationRepository).delete(v);
    }

    @Test
    @DisplayName("2.2 - Noto'g'ri kod bilan ro'yxatdan o'tish muvaffaqiyatsiz")
    void register_wrongCode_throwsException() {
        when(verificationRepository.findByEmailAndCode("new@gmail.com", "000000"))
                .thenReturn(Optional.empty());

        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@gmail.com");
        req.setCode("000000");
        req.setFullName("Test");
        req.setPassword("pass123");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyAndRegister(req));
        assertTrue(ex.getMessage().contains("Kod noto'g'ri"));
    }

    @Test
    @DisplayName("2.3 - Muddati o'tgan kod bilan ro'yxatdan o'tish muvaffaqiyatsiz")
    void register_expiredCode_throwsException() {
        EmailVerification v = new EmailVerification();
        v.setEmail("test@gmail.com");
        v.setCode("111111");
        v.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(verificationRepository.findByEmailAndCode("test@gmail.com", "111111"))
                .thenReturn(Optional.of(v));

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setCode("111111");
        req.setFullName("Test");
        req.setPassword("pass123");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyAndRegister(req));
        assertTrue(ex.getMessage().contains("muddati tugagan"));
    }


    @Test
    @DisplayName("3.1 - To'g'ri email va parol bilan login muvaffaqiyatli")
    void login_validCredentials_success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@gmail.com");
        user.setFullName("Test User");
        user.setPassword("hashed");
        user.setRole(User.Role.STUDENT);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken("user@gmail.com", "STUDENT")).thenReturn("token123");

        AuthRequest req = new AuthRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("pass123");

        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("token123", res.getToken());
        assertEquals("user@gmail.com", res.getEmail());
    }

    @Test
    @DisplayName("3.2 - Noto'g'ri parol bilan login muvaffaqiyatsiz")
    void login_wrongPassword_throwsException() {
        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPassword("hashed");
        user.setRole(User.Role.STUDENT);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AuthRequest req = new AuthRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("wrong");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(req));
        assertTrue(ex.getMessage().contains("noto'g'ri"));
    }

    @Test
    @DisplayName("3.3 - Mavjud bo'lmagan email bilan login muvaffaqiyatsiz")
    void login_unknownEmail_throwsException() {
        when(userRepository.findByEmail("notexist@gmail.com")).thenReturn(Optional.empty());

        AuthRequest req = new AuthRequest();
        req.setEmail("notexist@gmail.com");
        req.setPassword("any");

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }
}