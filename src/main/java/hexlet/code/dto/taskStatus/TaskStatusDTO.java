package hexlet.code.dto.taskStatus;

import hexlet.code.model.Task;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatusDTO {
    private long id;
    private String name;
    private String slug;
    private String createdAt;
    private List<Task> tasks;
}
