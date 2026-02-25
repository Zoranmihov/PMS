package PMS.user.Services;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import PMS.user.DTO.RegisterDTO;
import PMS.user.Enteties.User;
import PMS.user.Exceptions.ApiException;
import PMS.user.Repositories.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public String registerUser(RegisterDTO registerDTO) {
            Optional<User> user = userRepository.findByEmail(registerDTO.email());
            if (user.isPresent()) {
                throw new ApiException(409, "Email is already in use");
            } else {
                User newUser = new User (
                    registerDTO.email(), 
                    passwordEncoder().encode(registerDTO.password()),
                    registerDTO.name(),
                    new String[] {"USER"}
                );
                userRepository.save(newUser);
                return "Success";
            }   
    }
}   
