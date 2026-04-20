package uz.eduplatform.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.eduplatform.entity.User;
import uz.eduplatform.repository.UserRepository;

@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@gmail.com").ifPresent(userRepository::delete);

        User admin = new User();
        admin.setEmail("admin@gmail.com");
        admin.setFullName("Admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);
        System.out.println("✅ Admin yaratildi: admin@gmail.com / admin123");
    }
}
