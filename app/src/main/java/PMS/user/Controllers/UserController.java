package PMS.user.Controllers;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PMS.user.DTO.LoginDTO;
import PMS.user.DTO.LoginResponseDTO;
import PMS.user.DTO.RegisterDTO;
import PMS.user.Services.UserService;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public String getMethodName() {
        return new String("Test");
    }
    

    @PostMapping("/login")
    public LoginResponseDTO userLogin(@RequestBody LoginDTO loginDTO) {
        return new LoginResponseDTO("Hello", "Token goes here");
    }

    @PostMapping("/register")
    public ResponseEntity<String> userRegister(@RequestBody RegisterDTO registerDTO) {
        String message = userService.registerUser(registerDTO);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }
    
}
