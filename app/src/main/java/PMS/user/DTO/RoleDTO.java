package PMS.user.DTO;

import java.util.Set;

import PMS.user.Enteties.Role;

public record RoleDTO(
   Set<Role> roles
) {
    
}
