package PMS.user.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PMS.user.DTO.AdminCreateUserDTO;
import PMS.user.DTO.AdminUpdateAccountInfoDTO;
import PMS.user.DTO.DeleteUserAsAdminDTO;
import PMS.user.DTO.RoleDTO;
import PMS.user.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @PutMapping("/user/{id}/roles")
    public ResponseEntity<String> setRoles(
            @PathVariable Long id,
            @Valid @RequestBody RoleDTO request) {
        String message = userService.setRoles(id, request.roles());
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PostMapping("/user/create")
    public ResponseEntity<String> postMethodName(@RequestBody AdminCreateUserDTO adminCreateUserDTO) {
        String message = userService.createUserAsAdmin(adminCreateUserDTO);
        return new ResponseEntity<String>(message, HttpStatus.OK);
    }

    @PostMapping("/user/delete")
    public ResponseEntity<String> deleteUserAsAdmin(@Valid @RequestBody DeleteUserAsAdminDTO deleteUserAsAdminDTO) {
        String message = userService.deleteUserAsAdmin(deleteUserAsAdminDTO.email());
        return new ResponseEntity<String>(message, HttpStatus.OK);
    }

    @PutMapping("/user/update")
    public ResponseEntity<String> updateUserAsAdmin(@RequestBody AdminUpdateAccountInfoDTO adminUpdateAccountInfoDTO) {
       String message = userService.updateUserAccountInformationAsAdmin(adminUpdateAccountInfoDTO);

        return new ResponseEntity<String>(message, HttpStatus.OK);
    }

}
