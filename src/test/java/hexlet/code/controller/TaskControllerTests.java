package hexlet.code.controller;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.taskStatus.TaskStatusCreateDTO;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.mapper.TaskMapper;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import hexlet.code.util.ModelGenerator;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskControllerTests {
    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private ModelGenerator modelGenerator;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskStatusRepository taskStatusRepository;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private TaskMapper taskMapper;
    private User testUser;
    private TaskStatus testStatus;
    private Task testTask;
    private Label testLabel;
    private JwtRequestPostProcessor token;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity())
                .build();

        testUser = Instancio.of(modelGenerator.getUserModel()).create();
        userRepository.save(testUser);

        testStatus = Instancio.of(modelGenerator.getTaskStatusModel()).create();
        taskStatusRepository.save(testStatus);

        testTask = Instancio.of(modelGenerator.getTaskModel()).create();
        taskRepository.save(testTask);

        testLabel = Instancio.of(modelGenerator.getLabelModel()).create();
        labelRepository.save(testLabel);

        token = jwt().jwt(builder -> builder.subject(testUser.getEmail()));
    }

    @AfterEach
    public void clean() {
        taskRepository.deleteAll();
        taskStatusRepository.deleteAll();
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testIndex() throws Exception {
        var result = mockMvc.perform(get("/api/tasks").with(token))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray();
    }

    @Test
    public void testIndexWithFilter() throws Exception {
        var user = Instancio.of(modelGenerator.getUserModel())
                .create();
        userRepository.save(user);

        var status = new TaskStatus();
        status.setName("TestStatus");
        status.setSlug("forTest");
        taskStatusRepository.save(status);

        var label = new Label();
        label.setName("testLabel");
        labelRepository.save(label);

        var task = new Task();
        task.setName("TaskName");
        task.setAssignee(user);
        task.setTaskStatus(status);
        task.setLabels(Set.of(label));
        taskRepository.save(task);

        var result = mockMvc.perform(get(
                "/api/tasks?titleCont=TaskName&assigneeId={id}&status=forTest&labelId={labId}",
                        user.getId(), label.getId())
                        .with(token))
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertTrue(body.contains(user.getId().toString()));
        assertTrue(body.contains(status.getSlug()));
        assertTrue(body.contains(label.getId().toString()));
    }

    @Test
    public void testShow() throws Exception {
        var request = get("/api/tasks/" + testTask.getId()).with(token);
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).and(
                v -> v.node("title").isEqualTo(testTask.getName())
        );
    }

    @Test
    public void testCreate() throws Exception {
        var data = new TaskCreateDTO();
        data.setTitle("testTitle");
        data.setAssigneeId(testUser.getId());
        data.setStatus(testStatus.getSlug());

        var request = post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data))
                .with(token);

        var result = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();

        var body = result.getResponse().getContentAsString();
        var id = om.readTree(body).get("id").asLong();
        assertThat(taskRepository.findById(id)).isPresent();

        var testTask2 = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
        assertThat(testTask2).isNotNull();
        assertThat(testTask2.getName()).isEqualTo(data.getTitle());
        assertThat(testTask2.getAssignee().equals(testUser)).isTrue();
    }

    @Test
    public void testUpdate() throws Exception {
        var data = Map.of("title", "updateName");

        var request = put("/api/tasks/" + testTask.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isOk());

        var task = taskRepository.findById(testTask.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + testTask.getId() + " not found"));
        assertThat(task.getName()).isEqualTo(("updateName"));
    }

    @Test
    public void testDelete() throws Exception {
        var request = delete("/api/tasks/" + testTask.getId())
                .with(token);
        mockMvc.perform(request)
                .andExpect(status().isNoContent());
        Assertions.assertThat(taskRepository.existsById(testTask.getId())).isFalse();
    }

    @Test void testDeleteTaskStatusWithDependencies() throws Exception {
        var testTaskStatus2 = Instancio.of(modelGenerator.getTaskStatusModel()).create();
        var testTask = Instancio.of(modelGenerator.getTaskModel()).create();
        testTaskStatus2.setTasks(List.of(testTask));
        taskStatusRepository.save(testTaskStatus2);

        var request = delete("/api/task_statuses/" + testTaskStatus2.getId()).with(jwt());
        mockMvc.perform(request).andExpect(status().isNoContent());

        assertThat(taskStatusRepository.existsById(testTaskStatus2.getId()));
    }

    @Test
    public void testDeleteUserWithDependencies() throws Exception {
        var testUser2 = Instancio.of(modelGenerator.getUserModel()).create();
        userRepository.save(testUser2);
        var token2 = jwt().jwt(builder -> builder.subject(testUser2.getEmail()));

        var data = new TaskCreateDTO();
        data.setTitle("testTitle");
        data.setAssigneeId(testUser2.getId());
        data.setStatus(testStatus.getSlug());
        var testTask = taskMapper.map(data);
        taskRepository.save(testTask);

        var request = delete("/api/users/" + testUser2.getId()).with(token2);

        mockMvc.perform(request).andExpect(status().isMethodNotAllowed());

        assertTrue(userRepository.existsById(testUser2.getId()));
    }

    @Test
    public void testDeleteLabelWithDependencies() throws Exception {
        testTask.setLabels(Set.of(testLabel));
        taskRepository.save(testTask);
        var request = delete("/api/labels/" + testLabel.getId()).with(jwt());
        mockMvc.perform(request).andExpect(status().isMethodNotAllowed());

        assertTrue(labelRepository.existsById(testLabel.getId()));
    }
}
