package com.negzaoui.stuffing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration pour la sécurité Keycloak OAuth2.
 * L'authentification est déléguée à Keycloak : les endpoints protégés
 * doivent retourner 401 sans token Bearer valide.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void protectedEndpoint_shouldReturn401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/manager/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoint_shouldReturn200_orMethodNotAllowed() throws Exception {
        // Les endpoints publics restent accessibles sans token
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is2xxSuccessful());
    }
}
