package PMS.user.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserClaimsDTO {
    private Long id;
    private String email;
    private String[] roles;
}
