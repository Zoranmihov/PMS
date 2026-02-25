package PMS.user.DTO;


import lombok.Getter;

public record RegisterDTO(
    String email,
    String password,
    String name
) {}

