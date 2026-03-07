package PMS.user.Services;

import PMS.user.DTO.AdminCreateUserDTO;
import PMS.user.DTO.AdminUpdateAccountInfoDTO;
import PMS.user.DTO.LoginDTO;
import PMS.user.DTO.LoginResponseDTO;
import PMS.user.DTO.RegisterDTO;
import PMS.user.DTO.UpdateAccountInfoDTO;
import PMS.user.Enteties.Role;
import PMS.user.Enteties.User;
import PMS.user.Exceptions.ApiException;
import PMS.user.Repositories.UserRepository;
import PMS.user.Util.ActivationUtil;
import PMS.user.Util.DeactivationUtil;
import PMS.user.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ActivationUtil activationUtil;

    @Mock
    private DeactivationUtil deactivationUtil;

    @Mock
    private MailService mailService;

    @Mock
    private Claims claims;

    @InjectMocks
    private UserService userService;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    // registerUser

    @Test
    void registerUser_ShouldThrow409_WhenEmailAlreadyExists() {
        RegisterDTO dto = new RegisterDTO("existing@example.com", "Password1!", "Zoran");

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> userService.registerUser(dto));

        assertEquals(409, ex.getStatusCode());
        assertEquals("Email is already in use", ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(activationUtil, never()).generateActivationToken(anyString());
        verify(mailService, never()).sendActivationMail(anyString(), anyString());
    }

    @Test
    void registerUser_ShouldSaveInactiveUserWithEncodedPasswordAndSendActivationMail_WhenValid() {
        RegisterDTO dto = new RegisterDTO("new@example.com", "Password1!", "Zoran");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(activationUtil.generateActivationToken(dto.email())).thenReturn("activation-token");

        String result = userService.registerUser(dto);

        assertEquals("Success", result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        verify(mailService).sendActivationMail("new@example.com", "activation-token");

        User saved = captor.getValue();
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("Zoran", saved.getName());
        assertFalse(saved.isActive());
        assertTrue(saved.getRoles().contains(Role.USER));
        assertNotEquals("Password1!", saved.getPassword());
        assertTrue(passwordEncoder.matches("Password1!", saved.getPassword()));
    }

    // activateAccount

    @Test
    void activateAccount_ShouldActivateUserByEmail_WhenTokenIsValid() {
        when(activationUtil.validateAndGetActivationEmail("valid-token")).thenReturn("user@example.com");

        String result = userService.activateAccount("valid-token");

        assertEquals("Activated", result);
        verify(userRepository).activateByEmail("user@example.com");
    }

    // loginUser

    @Test
    void loginUser_ShouldThrow401_WhenUserDoesNotExist() {
        LoginDTO dto = new LoginDTO("missing@example.com", "Password1!");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> userService.loginUser(dto));

        assertEquals(401, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void loginUser_ShouldThrow401_WhenAccountIsNotActivated() {
        LoginDTO dto = new LoginDTO("user@example.com", "Password1!");

        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                false
        );
        user.setId(1L);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> userService.loginUser(dto));

        assertEquals(401, ex.getStatusCode());
        assertEquals("Account isn't activated", ex.getMessage());
    }

    @Test
    void loginUser_ShouldThrow401_WhenPasswordIsInvalid() {
        LoginDTO dto = new LoginDTO("user@example.com", "WrongPassword1!");

        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> userService.loginUser(dto));

        assertEquals(401, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void loginUser_ShouldReturnAccessAndRefreshToken_WhenCredentialsAreValid() {
        LoginDTO dto = new LoginDTO("user@example.com", "Password1!");

        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("refresh-token");

        LoginResponseDTO response = userService.loginUser(dto);

        assertEquals("Welcome", response.getMessage());
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    // refreshJwt

    @Test
    void refreshJwt_ShouldThrow401_WhenTokenIsInvalid() {
        when(jwtUtil.validateJwt("bad-token")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> userService.refreshJwt("bad-token"));

        assertEquals(401, ex.getStatusCode());
        assertEquals("Invalid token", ex.getMessage());
    }

    @Test
    void refreshJwt_ShouldThrow401_WhenTokenTypeIsNotRefresh() {
        when(jwtUtil.validateJwt("token")).thenReturn(true);
        when(jwtUtil.parseClaims("token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("access");

        ApiException ex = assertThrows(ApiException.class, () -> userService.refreshJwt("token"));

        assertEquals(401, ex.getStatusCode());
        assertEquals("Invalid token type", ex.getMessage());
    }

    @Test
    void refreshJwt_ShouldThrow401_WhenUserFromTokenDoesNotExist() {
        when(jwtUtil.validateJwt("token")).thenReturn(true);
        when(jwtUtil.parseClaims("token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("5");
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> userService.refreshJwt("token"));

        assertEquals(401, ex.getStatusCode());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void refreshJwt_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(5L);

        when(jwtUtil.validateJwt("token")).thenReturn(true);
        when(jwtUtil.parseClaims("token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("5");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles())).thenReturn("new-access-token");

        String result = userService.refreshJwt("token");

        assertEquals("new-access-token", result);
    }

    // requestAccountDeletion

    @Test
    void requestAccountDeletion_ShouldGenerateTokenAndSendMail() {
        when(deactivationUtil.generateDeactivationToken("user@example.com")).thenReturn("deactivation-token");

        String result = userService.requestAccountDeletion("user@example.com");

        assertEquals("Deactiation token was sent", result);
        verify(mailService).sendDelationMail("user@example.com", "deactivation-token");
    }

    // deleteAccount

    @Test
    void deleteAccount_ShouldDeleteUser_WhenTokenIsValidAndUserExists() {
        when(deactivationUtil.validateAndGetDeactivationEmail("valid-token")).thenReturn("user@example.com");
        when(userRepository.deleteByEmail("user@example.com")).thenReturn(1);

        String result = userService.deleteAccount("valid-token");

        assertEquals("Deleted", result);
        verify(userRepository).deleteByEmail("user@example.com");
    }

    @Test
    void deleteAccount_ShouldThrow404_WhenNoUserWasDeleted() {
        when(deactivationUtil.validateAndGetDeactivationEmail("valid-token")).thenReturn("user@example.com");
        when(userRepository.deleteByEmail("user@example.com")).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> userService.deleteAccount("valid-token"));

        assertEquals(404, ex.getStatusCode());
        assertEquals("User not found", ex.getMessage());
    }

    // updateAccountInformation

    @Test
    void updateAccountInformation_ShouldThrow400_WhenAllFieldsAreMissing() {
        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO(null, null, null);

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateAccountInformation("user@example.com", dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("At least one field is required.", ex.getMessage());
    }

    @Test
    void updateAccountInformation_ShouldThrow404_WhenUserDoesNotExist() {
        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO("new@example.com", null, null);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateAccountInformation("user@example.com", dto));

        assertEquals(404, ex.getStatusCode());
        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void updateAccountInformation_ShouldThrow400_WhenEmailFormatIsInvalid() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO("bad-email", null, null);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateAccountInformation("user@example.com", dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid email format", ex.getMessage());
    }

    @Test
    void updateAccountInformation_ShouldThrow400_WhenPasswordFormatIsInvalid() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO(null, "weak", null);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateAccountInformation("user@example.com", dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid password format", ex.getMessage());
    }

    @Test
    void updateAccountInformation_ShouldThrow400_WhenNameIsTooShort() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO(null, null, "ab");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateAccountInformation("user@example.com", dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid name format", ex.getMessage());
    }

    @Test
    void updateAccountInformation_ShouldUpdateOnlyName_WhenOnlyNameProvided() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Old Name",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO(null, null, "New Name");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginResponseDTO response = userService.updateAccountInformation("user@example.com", dto);

        assertEquals("Your information has been updated.", response.getMessage());
        assertNull(response.getToken());
        assertNull(response.getRefreshToken());
        assertEquals("New Name", user.getName());

        verify(userRepository).save(user);
    }

    @Test
    void updateAccountInformation_ShouldUpdateOnlyPassword_WhenOnlyPasswordProvided() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("OldPassword1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO(null, "Password1!", null);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginResponseDTO response = userService.updateAccountInformation("user@example.com", dto);

        assertEquals("Your information has been updated.", response.getMessage());
        assertTrue(passwordEncoder.matches("Password1!", user.getPassword()));
        assertNull(response.getToken());
        assertNull(response.getRefreshToken());

        verify(userRepository).save(user);
    }

    @Test
    void updateAccountInformation_ShouldUpdateEmailAndReturnNewTokens_WhenEmailProvided() {
        User user = new User(
                "old@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO("new@example.com", null, null);

        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(1L, "new@example.com", user.getRoles())).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("new-refresh-token");

        LoginResponseDTO response = userService.updateAccountInformation("old@example.com", dto);

        assertEquals("Your information has been updated.", response.getMessage());
        assertEquals("new-access-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("new@example.com", user.getEmail());

        verify(userRepository).save(user);
    }

    @Test
    void updateAccountInformation_ShouldUpdateAllFields_WhenAllProvidedAndValid() {
        User user = new User(
                "old@example.com",
                passwordEncoder.encode("OldPassword1!"),
                "Old Name",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        UpdateAccountInfoDTO dto = new UpdateAccountInfoDTO("new@example.com", "Password1!", "New Name");

        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(1L, "new@example.com", user.getRoles())).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("new-refresh-token");

        LoginResponseDTO response = userService.updateAccountInformation("old@example.com", dto);

        assertEquals("Your information has been updated.", response.getMessage());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("New Name", user.getName());
        assertTrue(passwordEncoder.matches("Password1!", user.getPassword()));
        assertEquals("new-access-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        verify(userRepository).save(user);
    }

    // createUserAsAdmin

    @Test
    void createUserAsAdmin_ShouldThrow409_WhenEmailAlreadyExists() {
        AdminCreateUserDTO dto = new AdminCreateUserDTO(
                "existing@example.com",
                "Password1!",
                "Zoran",
                true,
                EnumSet.of(Role.USER, Role.ADMIN)
        );

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> userService.createUserAsAdmin(dto));

        assertEquals(409, ex.getStatusCode());
        assertEquals("Email is already in use", ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserAsAdmin_ShouldSaveUserWithGivenRolesAndActivation_WhenValid() {
        AdminCreateUserDTO dto = new AdminCreateUserDTO(
                "admincreated@example.com",
                "Password1!",
                "Created User",
                true,
                EnumSet.of(Role.USER, Role.ADMIN)
        );

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);

        String result = userService.createUserAsAdmin(dto);

        assertEquals("Success", result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("admincreated@example.com", saved.getEmail());
        assertEquals("Created User", saved.getName());
        assertTrue(saved.isActive());
        assertTrue(saved.getRoles().contains(Role.ADMIN));
        assertTrue(saved.getRoles().contains(Role.USER));
        assertTrue(passwordEncoder.matches("Password1!", saved.getPassword()));
    }

    // deleteUserAsAdmin

    @Test
    void deleteUserAsAdmin_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.deleteByEmail("user@example.com")).thenReturn(1);

        String result = userService.deleteUserAsAdmin("user@example.com");

        assertEquals("Deleted", result);
        verify(userRepository).deleteByEmail("user@example.com");
    }

    @Test
    void deleteUserAsAdmin_ShouldThrow404_WhenUserDoesNotExist() {
        when(userRepository.deleteByEmail("missing@example.com")).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.deleteUserAsAdmin("missing@example.com"));

        assertEquals(404, ex.getStatusCode());
        assertEquals("User not found", ex.getMessage());
    }

    // updateUserAccountInformationAsAdmin

    @Test
    void updateUserAccountInformationAsAdmin_ShouldThrow400_WhenAllFieldsAreMissing() {
        AdminUpdateAccountInfoDTO dto = new AdminUpdateAccountInfoDTO(1L, null, null, null);

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUserAccountInformationAsAdmin(dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("At least one field is required.", ex.getMessage());
    }

    @Test
    void updateUserAccountInformationAsAdmin_ShouldThrow404_WhenUserDoesNotExist() {
        AdminUpdateAccountInfoDTO dto = new AdminUpdateAccountInfoDTO(99L, "new@example.com", null, null);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUserAccountInformationAsAdmin(dto));

        assertEquals(404, ex.getStatusCode());
        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void updateUserAccountInformationAsAdmin_ShouldThrow400_WhenEmailFormatIsInvalid() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );

        AdminUpdateAccountInfoDTO dto = new AdminUpdateAccountInfoDTO(1L, "bad-email", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUserAccountInformationAsAdmin(dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid email format", ex.getMessage());
    }

    @Test
    void updateUserAccountInformationAsAdmin_ShouldThrow400_WhenPasswordFormatIsInvalid() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );

        AdminUpdateAccountInfoDTO dto = new AdminUpdateAccountInfoDTO(1L, null, "weak", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUserAccountInformationAsAdmin(dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid password format", ex.getMessage());
    }

    @Test
    void updateUserAccountInformationAsAdmin_ShouldThrow400_WhenNameFormatIsInvalid() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );

        AdminUpdateAccountInfoDTO dto = new AdminUpdateAccountInfoDTO(1L, null, null, "ab");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUserAccountInformationAsAdmin(dto));

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid name format", ex.getMessage());
    }

    @Test
    void updateUserAccountInformationAsAdmin_ShouldUpdateFields_WhenInputIsValid() {
        User user = new User(
                "old@example.com",
                passwordEncoder.encode("OldPassword1!"),
                "Old Name",
                EnumSet.of(Role.USER),
                true
        );

        AdminUpdateAccountInfoDTO dto = new AdminUpdateAccountInfoDTO(
                1L,
                "new@example.com",
                "Password1!",
                "New Name"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String result = userService.updateUserAccountInformationAsAdmin(dto);

        assertEquals("Account information has been updated", result);
        assertEquals("new@example.com", user.getEmail());
        assertEquals("New Name", user.getName());
        assertTrue(passwordEncoder.matches("Password1!", user.getPassword()));

        verify(userRepository).save(user);
    }

    // setRoles

    @Test
    void setRoles_ShouldThrow404_WhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> userService.setRoles(1L, Set.of(Role.ADMIN)));

        assertEquals(404, ex.getStatusCode());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void setRoles_ShouldReplaceRolesAndAlwaysKeepUserRole_WhenRolesProvided() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.USER),
                true
        );
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String result = userService.setRoles(1L, Set.of(Role.ADMIN));

        assertEquals("Success", result);
        assertTrue(user.getRoles().contains(Role.ADMIN));
        assertTrue(user.getRoles().contains(Role.USER));
        assertEquals(2, user.getRoles().size());
    }

    @Test
    void setRoles_ShouldKeepOnlyUserRole_WhenRolesIsNull() {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("Password1!"),
                "Zoran",
                EnumSet.of(Role.ADMIN, Role.USER),
                true
        );
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String result = userService.setRoles(1L, null);

        assertEquals("Success", result);
        assertTrue(user.getRoles().contains(Role.USER));
        assertEquals(1, user.getRoles().size());
    }
}