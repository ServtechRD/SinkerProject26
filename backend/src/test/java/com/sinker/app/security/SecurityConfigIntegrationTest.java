package com.sinker.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.auth.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    // --- Public endpoint tests ---

    @Test
    void loginEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiAccessibleWithoutAuth() throws Exception {
        int statusCode = mockMvc.perform(get("/swagger-ui/index.html"))
                .andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(
                statusCode == 200 || statusCode == 302,
                "Swagger UI should be accessible (200 or 302), got: " + statusCode);
    }

    @Test
    void apiDocsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    // --- Protected endpoint tests ---

    @Test
    void protectedEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointReturns401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointReturns401WithExpiredToken() throws Exception {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-for-unit-tests-must-be-at-least-32-bytes!", -1000L);
        String expiredToken = shortLived.generateToken(1L, "admin", "admin");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        String token = tokenProvider.generateToken(1L, "admin", "admin");

        // /api/users doesn't have a controller yet, so we get 404 not 401
        int statusCode = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(401, statusCode,
                "Valid token should not return 401");
    }

    @Test
    void protectedEndpointReturns401WithMalformedAuthHeader() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "NotBearer sometoken"))
                .andExpect(status().isUnauthorized());
    }

    // --- CORS tests ---

    @Test
    void corsAllowsLocalhost5173() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsAllowsRemoteVmOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://192.168.1.100:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.1.100:5173"));
    }

    @Test
    void corsAllows127001Origin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

   /*
   FIXME
    @Test
    void corsRejectsDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                //.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
                .andExpect(header().string("Access-Control-Allow-Origin", not(equalTo("http://evil.com"))));
    }*/
}
