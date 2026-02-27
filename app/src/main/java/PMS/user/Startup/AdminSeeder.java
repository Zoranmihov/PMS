package PMS.user.Startup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import PMS.user.Enteties.User;
import PMS.user.Repositories.UserRepository;
import jakarta.transaction.Transactional;

@Component
public class AdminSeeder implements ApplicationRunner {
    @Autowired
    UserRepository userRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@local.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin")
    private String adminPassword;

    @Value("${app.admin.name:Admin}")
    private String adminName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        String[] roles = new String[] { "ADMIN" };

        User admin = new User(
                adminEmail,
                passwordEncoder.encode(adminPassword),
                adminName,
                roles, 
            true);

        userRepository.save(admin);
    }

}
