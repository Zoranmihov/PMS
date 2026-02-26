package PMS.user.Controllers;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PMS.user.DTO.LoginDTO;
import PMS.user.DTO.LoginResponseDTO;
import PMS.user.DTO.RefreshDTO;
import PMS.user.DTO.RegisterDTO;
import PMS.user.Services.UserService;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${jwt_expiration}")
    private int jwtExpiration;

    @GetMapping("/test")
    public String getMethodName() {
        return new String("Test");
    }
    

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> userLogin(@RequestBody LoginDTO loginDTO) {
        LoginResponseDTO loginResponseDTO = userService.loginUser(loginDTO);

        ResponseCookie cookie = ResponseCookie.from("access_token", loginResponseDTO.getToken())
            .httpOnly(true)
            .secure(true)          
            .sameSite("Lax")
            .path("/")
            .maxAge(jwtExpiration / 1000)
            .build();
       return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(loginResponseDTO);
    }

    @PostMapping("/register")
    public ResponseEntity<String> userRegister(@RequestBody RegisterDTO registerDTO) {
        String message = userService.registerUser(registerDTO);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshJwt(@RequestBody RefreshDTO refreshDTO) {
        String newAccessToken = userService.refreshJwt(refreshDTO.refreshtoken());

    ResponseCookie cookie = ResponseCookie.from("access_token", newAccessToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(jwtExpiration / 1000)
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(newAccessToken);
    }
}
