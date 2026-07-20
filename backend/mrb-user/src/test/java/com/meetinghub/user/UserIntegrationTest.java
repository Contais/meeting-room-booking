package com.meetinghub.user;

import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = UserApplication.class
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.server-addr=",
        "spring.cloud.nacos.discovery.server-addr=",
        "spring.config.import=optional:nacos:"
})
class UserIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        userRepository.delete(null);
    }

    private ResponseEntity<Map<String, Object>> post(String path, Map<String, String> body) {
        return restTemplate.exchange(
                baseUrl + path, HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<>() {}
        );
    }

    private ResponseEntity<Map<String, Object>> get(String path) {
        return restTemplate.exchange(
                baseUrl + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}
        );
    }

    @Test
    void should_registerUser_when_validData() {
        Map<String, String> body = Map.of(
                "username", "newuser",
                "password", "password123",
                "phone", "13800000001"
        );

        ResponseEntity<Map<String, Object>> response = post("/user/register", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isNotNull();

        User saved = userRepository.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "newuser")
        );
        assertThat(saved).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
    }

    @Test
    void should_returnError_when_duplicateUsername() {
        User existing = new User();
        existing.setUsername("existing");
        existing.setPassword("hash");
        existing.setStatus(1);
        userRepository.insert(existing);

        Map<String, String> body = Map.of(
                "username", "existing",
                "password", "pass",
                "phone", "13800000002"
        );

        ResponseEntity<Map<String, Object>> response = post("/user/register", body);

        assertThat(response.getBody()).isNotNull();
        Integer code = (Integer) response.getBody().get("code");
        assertThat(code).isNotEqualTo(200);
    }

    @Test
    void should_returnUser_when_queryByUsername() {
        User user = new User();
        user.setUsername("querytest");
        user.setPassword("hash");
        user.setPhone("13800000003");
        user.setStatus(1);
        userRepository.insert(user);

        ResponseEntity<Map<String, Object>> response = get("/user/info/username/querytest");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isNotNull();
    }

    @Test
    void should_returnNullData_when_usernameNotFound() {
        ResponseEntity<Map<String, Object>> response = get("/user/info/username/nobody");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("data")).isNull();
    }

    @Test
    void should_returnError_when_invalidUsernameFormat() {
        Map<String, String> body = Map.of(
                "username", "a",
                "password", "pass",
                "phone", "13800000004"
        );

        ResponseEntity<Map<String, Object>> response = post("/user/register", body);

        assertThat(response.getBody()).isNotNull();
        Integer code = (Integer) response.getBody().get("code");
        assertThat(code).isNotEqualTo(200);
    }
}
