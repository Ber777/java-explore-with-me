package ewm.user.dto;

import lombok.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserDto {
    @NotBlank(message = "Email не должен быть пустым")
    @Email(message = "Некорректный формат email")
    @Size(min = 6, max = 254, message = "Email должен быть от 6 до 254 символов")
    private String email;

    @NotBlank(message = "Имя не должно быть пустым")
    @Size(min = 2, max = 250, message = "Имя должно быть от 2 до 250 символов")
    private String name;
}
