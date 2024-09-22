package hexlet.code.dto.taskStatus;

import hexlet.code.model.Task;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatusCreateDTO {

    @NotBlank
    private String name;
    @NotBlank
    private String slug;
    private List<Task> tasks;

}
