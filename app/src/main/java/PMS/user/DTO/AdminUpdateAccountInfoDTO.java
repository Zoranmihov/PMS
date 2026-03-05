package PMS.user.DTO;

public record AdminUpdateAccountInfoDTO(
    Long userId,
    String email,
    String password,
    String name
) {
}
