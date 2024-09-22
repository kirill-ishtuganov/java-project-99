package hexlet.code.dto.taskStatus;

import hexlet.code.model.Task;
import jakarta.validation.constraints.NotBlank;
import org.openapitools.jackson.nullable.JsonNullable;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TaskStatusUpdateDTO {

    @NotBlank
    private JsonNullable<String> name;
    @NotBlank
    private JsonNullable<String> slug;
    private JsonNullable<List<Task>> tasks;

}
