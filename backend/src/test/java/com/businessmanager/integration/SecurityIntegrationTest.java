package com.businessmanager.integration;

import com.businessmanager.entity.Role;
import com.businessmanager.entity.User;
import com.businessmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste de bout en bout la chaîne sécurité : login, JWT, refresh, rôles, validation. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createUsers() {
        ensureUser("admin@test.fr", Role.ADMIN);
        ensureUser("employee@test.fr", Role.EMPLOYEE);
        ensureUser("accounting@test.fr", Role.ACCOUNTING);
    }

    private void ensureUser(String email, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder().firstName("Test").lastName(role.name())
                    .email(email).password(passwordEncoder.encode(PASSWORD)).role(role).build());
        }
    }

    private JsonNode login(String email) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", PASSWORD));
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String bearer(String email) throws Exception {
        return "Bearer " + login(email).get("accessToken").asText();
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", "admin@test.fr", "password", "wrong-pass"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401_notServerError() throws Exception {
        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_cannotBeUsedAsAccessToken() throws Exception {
        String refresh = login("admin@test.fr").get("refreshToken").asText();
        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returnsNewTokens_andRejectsAccessTokens() throws Exception {
        JsonNode tokens = login("admin@test.fr");

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("accessToken").asText() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicRegistration_doesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.fr\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_canCreateAndListCustomers() throws Exception {
        String token = bearer("admin@test.fr");

        mockMvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jean Dupont\",\"company\":\"ACME\",\"email\":\"jean@acme.fr\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        mockMvc.perform(get("/api/customers").param("search", "dupont").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Jean Dupont"));
    }

    @Test
    void createCustomer_withInvalidEmail_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/customers").header("Authorization", bearer("admin@test.fr"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"pas-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void employee_cannotAccessFinance() throws Exception {
        mockMvc.perform(get("/api/quotes").header("Authorization", bearer("employee@test.fr")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/invoices").header("Authorization", bearer("employee@test.fr")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/stats/dashboard").header("Authorization", bearer("employee@test.fr")))
                .andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotWriteCustomers() throws Exception {
        mockMvc.perform(post("/api/customers").header("Authorization", bearer("employee@test.fr"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@x.fr\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accounting_cannotAccessProjects_butCanAccessInvoices() throws Exception {
        String token = bearer("accounting@test.fr");
        mockMvc.perform(get("/api/projects").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/invoices").header("Authorization", token)).andExpect(status().isOk());
    }

    @Test
    void userAdministration_isAdminOnly() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer("employee@test.fr")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", bearer("admin@test.fr")))
                .andExpect(status().isOk());
        // La liste des collaborateurs assignables reste ouverte à tous les rôles
        mockMvc.perform(get("/api/users/assignable").header("Authorization", bearer("employee@test.fr")))
                .andExpect(status().isOk());
    }

    @Test
    void dashboard_isAvailableForAdmin() throws Exception {
        mockMvc.perform(get("/api/stats/dashboard").header("Authorization", bearer("admin@test.fr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRevenue.length()").value(12));
    }
}
