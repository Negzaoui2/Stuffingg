package com.negzaoui.stuffing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.negzaoui.stuffing.dto.auth.PasswordResetConfirmRequest;
import com.negzaoui.stuffing.dto.auth.PasswordResetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test du reset password.
 * Le endpoint /api/auth/reset-password/* est public (permitAll),
 * donc pas besoin de token Keycloak pour le tester.
 * On utilise le compte admin@soprahr.com crÃ©Ã© par DataInitializer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void resetPassword_flow_shouldWork() throws Exception {
        // Le DataInitializer crÃ©e admin@soprahr.com au dÃ©marrage
        String email = "admin@soprahr.com";

        var req = PasswordResetRequest.builder().email(email).build();

        String raw = mockMvc.perform(post("/api/auth/reset-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(raw);
        // Si l'email existe, on reÃ§oit un token
        if (json.hasNonNull("token") && !json.get("token").asText().isBlank()) {
            String token = json.get("token").asText();

            var confirm = PasswordResetConfirmRequest.builder()
                    .token(token)
                    .newPassword("NewPassword123!")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirm)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Mot de passe mis Ã  jour")));
        }
    }
}
