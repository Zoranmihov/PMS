package PMS.user.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DeleteUserAsAdminDTO(
    @NotBlank @Email(message = "Bad email format") String email
) {
}
