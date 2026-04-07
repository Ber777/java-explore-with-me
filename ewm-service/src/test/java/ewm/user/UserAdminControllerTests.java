package ewm.user;

import ewm.user.dto.*;
import ewm.exception.*;
import ewm.user.service.UserService;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.MediaType;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTests {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserAdminController userAdminController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userAdminController)
                .setControllerAdvice(new ErrorHandler())
                .setValidator(new LocalValidatorFactoryBean()) // добавляем валидатор
                .build();
    }

    @Test
    void shouldCreateUserAndReturnUserDto() throws Exception {
        NewUserDto request = NewUserDto.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        UserDto expectedResponse = UserDto.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        // Используем матчер Mockito для корректной заглушки
        when(userService.createUser(any(NewUserDto.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));

        // В verify также используем матчер для корректной проверки вызова
        verify(userService, times(1)).createUser(any(NewUserDto.class));
    }

    @Test
    void shouldReturnBadRequestWithErrorResponse() throws Exception {
        NewUserDto request = NewUserDto.builder()
                .email("invalid-email")
                .name("Test User")
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithErrorResponseWithEmptyName() throws Exception {
        NewUserDto request = NewUserDto.builder()
                .email("test@example.com")
                .name("")
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithErrorResponseWithShortName() throws Exception {
        NewUserDto request = NewUserDto.builder()
                .email("test@example.com")
                .name("A")
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnAllUsersWithPagination() throws Exception {
        UserDto user1 = UserDto.builder().id(1L).name("User 1").email("user1@example.com").build();
        UserDto user2 = UserDto.builder().id(2L).name("User 2").email("user2@example.com").build();

        List<UserDto> expectedUsers = List.of(user1, user2);

        when(userService.getUsers(null, 0, 10)).thenReturn(expectedUsers);

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("User 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("User 2"));
    }

    @Test
    void shouldReturnUsersByIds() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        UserDto user1 = UserDto.builder().id(1L).name("User 1").email("user1@example.com").build();
        UserDto user2 = UserDto.builder().id(2L).name("User 2").email("user2@example.com").build();

        List<UserDto> expectedUsers = List.of(user1, user2);

        when(userService.getUsers(ids, 0, 10)).thenReturn(expectedUsers);

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1", "2")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void shouldDeleteUserSuccessfully() throws Exception {
        Long userId = 1L;

        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/admin/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    void shouldReturnNotFoundOnDeleteUser() throws Exception {
        Long userId = 999L;

        doThrow(new NotFoundException("Пользователь", userId))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete("/admin/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnAllUsersWithMissingIdsParameter() throws Exception {
        UserDto user1 = UserDto.builder().id(1L).name("User 1").email("user1@example.com").build();
        UserDto user2 = UserDto.builder().id(2L).name("User 2").email("user2@example.com").build();

        List<UserDto> expectedUsers = List.of(user1, user2);

        when(userService.getUsers(null, 0, 10)).thenReturn(expectedUsers);

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void shouldReturnBadRequestWithNullEmail() throws Exception {
        NewUserDto request = NewUserDto.builder()
                .email(null)
                .name("Test User")
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithNullName() throws Exception {
        NewUserDto request = NewUserDto.builder()
                .email("test@example.com")
                .name(null)
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "invalid")
                        .param("size", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").exists())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"));
    }
}