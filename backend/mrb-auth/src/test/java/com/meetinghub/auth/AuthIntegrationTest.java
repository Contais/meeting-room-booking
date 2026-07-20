package com.meetinghub.auth;

import cn.hutool.crypto.digest.BCrypt;
import com.meetinghub.auth.feign.UserFeignClient;
import com.meetinghub.common.model.dto.AuthUserDTO;
import com.meetinghub.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AuthApplication.class
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.server-addr=",
        "spring.cloud.nacos.discovery.server-addr=",
        "spring.config.import=optional:nacos:"
})
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserFeignClient userFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        redisTemplate.delete(redisTemplate.keys("mrb:user:token:*"));
    }

    private AuthUserDTO createMockUser(Long id, String username, String password, int status) {
        AuthUserDTO user = new AuthUserDTO();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setStatus(status);
        return user;
    }

    @Test
    void should_returnToken_when_loginWithCorrectCredentials() {
        AuthUserDTO mockUser = createMockUser(1L, "testuser", "pass123", 1);
        when(userFeignClient.getUserForAuth("testuser")).thenReturn(Result.ok(mockUser));

        Map<String, String> body = Map.of("username", "testuser", "password", "pass123");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        String token = (String) response.getBody().get("data");
        assertThat(token).isNotBlank();
        assertThat(redisTemplate.hasKey("mrb:user:token:1")).isTrue();
    }

    @Test
    void should_returnError_when_loginWithWrongPassword() {
        AuthUserDTO mockUser = createMockUser(1L, "testuser", "correctpass", 1);
        when(userFeignClient.getUserForAuth("testuser")).thenReturn(Result.ok(mockUser));

        Map<String, String> body = Map.of("username", "testuser", "password", "wrongpass");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
        assertThat(response.getBody().get("message")).isEqualTo("用户名或密码错误");
    }

    @Test
    void should_returnError_when_loginWithNonexistentUser() {
        when(userFeignClient.getUserForAuth("nouser")).thenReturn(Result.ok(null));

        Map<String, String> body = Map.of("username", "nouser", "password", "pass");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(1005);
    }

    @Test
    void should_returnNewToken_when_refreshValidToken() {
        AuthUserDTO mockUser = createMockUser(1L, "testuser", "pass", 1);
        when(userFeignClient.getUserForAuth("testuser")).thenReturn(Result.ok(mockUser));

        Map<String, String> loginBody = Map.of("username", "testuser", "password", "pass");
        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(loginBody);
        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
                baseUrl + "/auth/login", HttpMethod.POST, loginRequest,
                new ParameterizedTypeReference<>() {}
        );
        String oldToken = (String) loginResponse.getBody().get("data");

        Map<String, String> refreshBody = Map.of("token", oldToken);
        HttpEntity<Map<String, String>> refreshRequest = new HttpEntity<>(refreshBody);
        ResponseEntity<Map<String, Object>> refreshResponse = restTemplate.exchange(
                baseUrl + "/auth/refresh", HttpMethod.POST, refreshRequest,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newToken = (String) refreshResponse.getBody().get("data");
        assertThat(newToken).isNotBlank();
        assertThat(newToken).isNotEqualTo(oldToken);
    }

    @Test
    void should_returnError_when_refreshInvalidToken() {
        Map<String, String> body = Map.of("token", "invalid.token.here");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/auth/refresh", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(1007);
    }

    @Test
    void should_deleteToken_when_logout() {
        AuthUserDTO mockUser = createMockUser(1L, "testuser", "pass", 1);
        when(userFeignClient.getUserForAuth("testuser")).thenReturn(Result.ok(mockUser));

        Map<String, String> loginBody = Map.of("username", "testuser", "password", "pass");
        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(loginBody);
        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
                baseUrl + "/auth/login", HttpMethod.POST, loginRequest,
                new ParameterizedTypeReference<>() {}
        );
        String token = (String) loginResponse.getBody().get("data");
        assertThat(redisTemplate.hasKey("mrb:user:token:1")).isTrue();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> logoutRequest = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> logoutResponse = restTemplate.exchange(
                baseUrl + "/auth/logout", HttpMethod.POST, logoutRequest,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(redisTemplate.hasKey("mrb:user:token:1")).isFalse();
    }
}
