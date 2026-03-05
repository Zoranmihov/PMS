package PMS.user.Services;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import PMS.user.DTO.AdminCreateUserDTO;
import PMS.user.DTO.AdminUpdateAccountInfoDTO;
import PMS.user.DTO.LoginDTO;
import PMS.user.DTO.LoginResponseDTO;
import PMS.user.DTO.RegisterDTO;
import PMS.user.DTO.UpdateAccountInfoDTO;
import PMS.user.Enteties.Role;
import PMS.user.Enteties.User;
import PMS.user.Exceptions.ApiException;
import PMS.user.Repositories.UserRepository;
import PMS.user.Util.ActivationUtil;
import PMS.user.Util.DeactivationUtil;
import PMS.user.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    ActivationUtil activationUtil;

    @Autowired
    DeactivationUtil deactivationUtil;

    @Autowired
    MailService mailService;

    private static final Pattern emailRegex = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern passwordRegex = Pattern
            .compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)(?=.*[^A-Za-z0-9]).{8,20}$");

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
                    EnumSet.of(Role.USER),
                    false);
            String activationToken = activationUtil.generateActivationToken(registerDTO.email());
            mailService.sendActivationMail(registerDTO.email(), activationToken);
            userRepository.save(newUser);

            return "Success";
        }
    }

    public String activateAccount(String token) {
        String email = activationUtil.validateAndGetActivationEmail(token);
        userRepository.activateByEmail(email);
        return "Activated";
    }

    public LoginResponseDTO loginUser(LoginDTO loginDTO) {

        User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new ApiException(401, "Invalid credentials"));

        if (!user.isActive()) {
            throw new ApiException(401, "Account isn't activated");
        }
        if (!passwordEncoder().matches(loginDTO.password(), user.getPassword())) {
            throw new ApiException(401, "Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new LoginResponseDTO("Welcome", token, refreshToken);
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

    public String requestAccountDeletion(String email) {
        String deactivationToken = deactivationUtil.generateDeactivationToken(email);
        mailService.sendDelationMail(email, deactivationToken);
        return "Deactiation token was sent";
    }

    @Transactional
    public String deleteAccount(String token) {
        String email = deactivationUtil.validateAndGetDeactivationEmail(token);
        int deleted = userRepository.deleteByEmail(email);

        if (deleted == 0) {
            throw new ApiException(404, "User not found");
        }
        return "Deleted";
    }

    public LoginResponseDTO updateAccountInformation(String userEmail, UpdateAccountInfoDTO updateAccountInfoDTO) {

        String email = updateAccountInfoDTO.email();
        String password = updateAccountInfoDTO.password();
        String name = updateAccountInfoDTO.name();

        if ((email == null || email.isBlank()) &&
                (password == null || password.isBlank()) &&
                (name == null || name.isBlank())) {
            throw new ApiException(400, "At least one field is required.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(404, "Account not found"));

        LoginResponseDTO responseDTO = new LoginResponseDTO(null, null, null);

        if (email != null && !email.isBlank()) {

            if (!emailRegex.matcher(email).matches()) {
                throw new ApiException(400, "Invalid email format");
            }

            String token = jwtUtil.generateToken(user.getId(), email, user.getRoles());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            responseDTO.setToken(token);
            responseDTO.setRefreshToken(refreshToken);

            user.setEmail(email);
        }

        if (password != null && !password.isBlank()) {

            if (!passwordRegex.matcher(password).matches()) {
                throw new ApiException(400, "Invalid password format");
            }
            user.setPassword(passwordEncoder().encode(password));
        }

        if (name != null && !name.isBlank()) {

            if (name.length() >= 3 && name.length() <= 50) {
                user.setName(name);
            } else {
                throw new ApiException(400, "Invalid name format");
            }

            user.setName(name);
        }

        userRepository.save(user);
        responseDTO.setMessage("Your information has been updated.");

        return responseDTO;
    }

    // Admin functions

    public String createUserAsAdmin(AdminCreateUserDTO adminCreateUserDTO) {
        if (userRepository.existsByEmail(adminCreateUserDTO.email())) {
            throw new ApiException(409, "Email is already in use");
        }

        User newUser = new User(
                adminCreateUserDTO.email(),
                passwordEncoder().encode(adminCreateUserDTO.password()),
                adminCreateUserDTO.name(),
                adminCreateUserDTO.roles(),
                adminCreateUserDTO.activated());

        userRepository.save(newUser);

        return "Success";
    }

    public String deleteUserAsAdmin(String email) {
        int deleted = userRepository.deleteByEmail(email);

        if (deleted == 0) {
            throw new ApiException(404, "User not found");
        }
        return "Deleted";
    }

    public String updateUserAccountInformationAsAdmin(AdminUpdateAccountInfoDTO updateAccountInfoDTO) {

        String email = updateAccountInfoDTO.email();
        String password = updateAccountInfoDTO.password();
        String name = updateAccountInfoDTO.name();

        if ((email == null || email.isBlank()) &&
                (password == null || password.isBlank()) &&
                (name == null || name.isBlank())) {
            throw new ApiException(400, "At least one field is required.");
        }

        User user = userRepository.findById(updateAccountInfoDTO.userId())
                .orElseThrow(() -> new ApiException(404, "Account not found"));

        if (email != null && !email.isBlank()) {

            if (!emailRegex.matcher(email).matches()) {
                throw new ApiException(400, "Invalid email format");
            }

            user.setEmail(email);
        }

        if (password != null && !password.isBlank()) {

            if (!passwordRegex.matcher(password).matches()) {
                throw new ApiException(400, "Invalid password format");
            }
            user.setPassword(passwordEncoder().encode(password));
        }

        if (name != null && !name.isBlank()) {

            if (name.length() >= 3 && name.length() <= 50) {
                user.setName(name);
            } else {
                throw new ApiException(400, "Invalid name format");
            }

            user.setName(name);
        }

        userRepository.save(user);

        return "Account information has been updated";
    }

    @Transactional
    public String setRoles(Long userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "User not found"));
        user.getRoles().clear();
        if (roles != null) {
            user.getRoles().addAll(roles);
        }
        user.getRoles().add(Role.USER);
        return "Success";
    }
}
