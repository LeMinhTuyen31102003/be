package be.spring.vanconhung.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.teacher.username}")
    private String seedUsername;

    @Value("${app.seed.teacher.password}")
    private String seedPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(seedUsername)) {
            return;
        }

        User teacher = new User();
        teacher.setUsername(seedUsername);
        teacher.setPassword(passwordEncoder.encode(seedPassword));
        teacher.setFullName("Cô Nhung");
        teacher.setRole(Role.TEACHER);
        teacher.setEnabled(true);
        userRepository.save(teacher);

        log.info("Đã tạo tài khoản giáo viên mặc định: username='{}', password='{}'. " +
                "Hãy đổi mật khẩu sau khi đăng nhập lần đầu.", seedUsername, seedPassword);
    }
}
