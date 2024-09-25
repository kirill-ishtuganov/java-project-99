package hexlet.code.controller;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import hexlet.code.dto.user.UserDTO;
import hexlet.code.dto.user.UserUpdateDTO;
import hexlet.code.mapper.UserMapper;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import hexlet.code.util.ModelGenerator;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


@SpringBootTest
@AutoConfigureMockMvc
public class UsersControllerTest {
    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelGenerator modelGenerator;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UserMapper userMapper;
    private JwtRequestPostProcessor token;
    private User testUser;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity())
                .build();

        token = jwt().jwt(builder -> builder.subject("hexlet@example.com"));

        testUser = Instancio.of(modelGenerator.getUserModel()).create();
        userRepository.save(testUser);
    }

    @AfterEach
    public void clean() {
        userRepository.deleteAll();
    }

    @Test
    public void testGetAll() throws Exception {
        var response = mockMvc.perform(get("/api/users").with(jwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();

        List<UserDTO> userDTOS = om.readValue(body, new TypeReference<>() { });

        var actual = userDTOS.stream().map(userMapper::map).toList();
        var expected = userRepository.findAll();
        Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetById() throws Exception {
        var request = get("/api/users/" + testUser.getId()).with(jwt());
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();
        assertThatJson(body).and(
                v -> v.node("email").isEqualTo(testUser.getEmail()),
                v -> v.node("firstName").isEqualTo(testUser.getFirstName()),
                v -> v.node("lastName").isEqualTo(testUser.getLastName()));
    }

    @Test
    public void testCreate() throws Exception {
        var data = Instancio.of(modelGenerator.getUserModel()).create();

        var request = post("/api/users")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(data.getEmail()).get();

        assertNotNull(user);
        assertThat(user.getFirstName()).isEqualTo(data.getFirstName());
        assertThat(user.getLastName()).isEqualTo(data.getLastName());
    }

    @Test
    public void testUpdate() throws Exception {
        var data = Map.of("firstName", "Mike");
        var token2 = jwt().jwt(builder -> builder.subject(testUser.getEmail()));

        var request = put("/api/users/" + testUser.getId())
                .with(token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isOk());

        var user = userRepository.findById(testUser.getId()).get();
        assertThat(user.getFirstName()).isEqualTo(("Mike"));
    }

    @Test
    public void testDestroy() throws Exception {
        var testUser2 = Instancio.of(modelGenerator.getUserModel()).create();
        userRepository.save(testUser2);
        var token2 = jwt().jwt(builder -> builder.subject(testUser2.getEmail()));

        var request = delete("/api/users/" + testUser2.getId())
                .with(token2);

        mockMvc.perform(request).andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(testUser2.getId()));
    }

    @Test
    public void testUpdateAnotherUser() throws Exception {
        var user = Instancio.of(modelGenerator.getUserModel())
                .create();
        userRepository.save(user);

        var newFirstName = "updated firstname";
        var updatedDTO = new UserUpdateDTO();
        updatedDTO.setFirstName(JsonNullable.of(newFirstName));

        var request = put("/api/users/{id}", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(updatedDTO))
                .with(token);

        mockMvc.perform(request).andExpect(status().isForbidden());

        var userFromRepo = userRepository.findByEmail(user.getEmail()).get();

        assertThat(userFromRepo).isNotNull();
        assertThat(userFromRepo.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(userFromRepo.getLastName()).isEqualTo(user.getLastName());
        assertThat(userFromRepo.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    public void testDestroyAnotherUser() throws Exception {
        var testUser2 = Instancio.of(modelGenerator.getUserModel())
                .create();
        userRepository.save(testUser2);

        var request = delete("/api/users/{id}", testUser2.getId()).with(token);
        mockMvc.perform(request)
                .andExpect(status().isForbidden());

        assertTrue(userRepository.existsById(testUser2.getId()));
    }
}
