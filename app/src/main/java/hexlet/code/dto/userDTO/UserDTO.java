package hexlet.code.dto.userDTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDTO {
    private Long id;

    private String email;

    private String firstName;

    private String lastName;
}
