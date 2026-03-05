package PMS.user.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
    @NotBlank @Email(message = "Bad email format") String email,
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$", message = "Password must be 8-20 chars and include upper, lower, number, and special character") String password,
    @NotBlank @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters long") String name) {
}
