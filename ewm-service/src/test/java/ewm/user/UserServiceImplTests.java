package ewm.user;

import ewm.user.dto.*;
import ewm.user.model.User;
import ewm.user.service.UserServiceImpl;
import ewm.user.repository.UserRepository;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateUserAndReturnUserDto() {
        NewUserDto request = NewUserDto.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        User expectedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        UserDto expectedDto = UserDto.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getName(), result.getName());
        assertEquals(expectedDto.getEmail(), result.getEmail());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldReturnAllUsersWithPagination() {
        int from = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(0, size);

        List<User> users = List.of(
                User.builder().id(1L).name("User1").email("user1@example.com").build(),
                User.builder().id(2L).name("User2").email("user2@example.com").build()
        );

        List<UserDto> expectedDtos = users.stream()
                .map(UserMapper::toUserDto)
                .toList();

        when(userRepository.findAllWithPageable(pageable)).thenReturn(users);

        List<UserDto> result = userService.getUsers(null, from, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedDtos.get(0).getId(), result.get(0).getId());
        assertEquals(expectedDtos.get(1).getName(), result.get(1).getName());

        verify(userRepository, times(1)).findAllWithPageable(pageable);
        verify(userRepository, never()).findByIdInWithPageable(any(), any());
    }

    @Test
    void shouldReturnUsersByIdsWithPagination() {
        List<Long> ids = List.of(1L, 2L);
        int from = 0;
        int size = 5;
        Pageable pageable = PageRequest.of(0, size);

        List<User> users = List.of(
                User.builder().id(1L).name("User1").email("user1@example.com").build()
        );

        List<UserDto> expectedDtos = users.stream()
                .map(UserMapper::toUserDto)
                .toList();

        when(userRepository.findByIdInWithPageable(ids, pageable)).thenReturn(users);

        List<UserDto> result = userService.getUsers(ids, from, size);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDtos.getFirst().getId(), result.getFirst().getId());

        verify(userRepository, times(1)).findByIdInWithPageable(ids, pageable);
        verify(userRepository, never()).findAllWithPageable(any());
    }

    @Test
    void shouldReturnAllUsers() {
        List<Long> emptyIds = List.of();
        int from = 5;
        int size = 15;
        Pageable pageable = PageRequest.of(from / size, size);

        List<User> users = List.of(
                User.builder().id(3L).name("User3").email("user3@example.com").build()
        );

        List<UserDto> expectedDtos = users.stream()
                .map(UserMapper::toUserDto)
                .toList();

        when(userRepository.findAllWithPageable(pageable)).thenReturn(users);

        List<UserDto> result = userService.getUsers(emptyIds, from, size);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDtos.getFirst().getId(), result.getFirst().getId());

        verify(userRepository, times(1)).findAllWithPageable(pageable);
        verify(userRepository, never()).findByIdInWithPageable(any(), any());
    }

    @Test
    void shouldDeleteUserById() {
        Long userId = 1L;
        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void shouldCallFindAllWithPageable() {
        int from = 10;
        int size = 20;
        Pageable pageable = PageRequest.of(from / size, size);

        List<User> users = List.of(
                User.builder().id(4L).name("User4").email("user4@example.com").build(),
                User.builder().id(5L).name("User5").email("user5@example.com").build()
        );

        when(userRepository.findAllWithPageable(pageable)).thenReturn(users);

        List<UserDto> result = userService.getUsers(null, from, size);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(userRepository, times(1)).findAllWithPageable(pageable);
    }
}
