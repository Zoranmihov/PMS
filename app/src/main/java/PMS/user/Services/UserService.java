package PMS.user.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import PMS.user.DTO.LoginDTO;
import PMS.user.DTO.LoginResponseDTO;
import PMS.user.DTO.RegisterDTO;
import PMS.user.Enteties.User;
import PMS.user.Exceptions.ApiException;
import PMS.user.JwtUtil.JwtUtil;
import PMS.user.Repositories.UserRepository;
import io.jsonwebtoken.Claims;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public String registerUser(RegisterDTO registerDTO) {
        if (userRepository.existsByEmail(registerDTO.email())) {
            throw new ApiException(409, "Email is already in use");
        } else {
            User newUser = new User(
                    registerDTO.email(),
                    passwordEncoder().encode(registerDTO.password()),
                    registerDTO.name(),
                    new String[] { "USER", "ADMIN" });
            userRepository.save(newUser);
            return "Success";
        }
    }

    public LoginResponseDTO loginUser(LoginDTO loginDTO) {

        User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new ApiException(401, "Invalid credentials"));

        if (!passwordEncoder().matches(loginDTO.password(), user.getPassword())) {
            throw new ApiException(401, "Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new LoginResponseDTO(null, token, refreshToken);
    }

    public String refreshJwt(String refreshToken) {
        if (!jwtUtil.validateJwt(refreshToken)) {
            new ApiException(401, "Invalid token");
        }

        Claims claims = jwtUtil.parseClaims(refreshToken);

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new ApiException(401, "Invalid token type");
        }

        Long userId = Long.valueOf(claims.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(401, "User not found"));

        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles());
    }
}
