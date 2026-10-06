package de.mmbbs.kassensystem.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mmbbs.kassensystem.backend.security.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "kassensystem.db.path=target/test-kassensystem-accounts.db",
        "kassensystem.auth.demo=true",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.de.mmbbs=WARN",
        "logging.level.org.springframework=WARN"
})
@AutoConfigureMockMvc
class AccountSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountService accounts;

    @Test
    void einzelrechtSchlaegtRolleUndSperreEntziehtSitzung() throws Exception {
        MockHttpSession admin = login("admin", "1234");
        String username = unique();
        MvcResult created = mvc.perform(post("/api/admin/users").session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("username", username, "displayName", "Testkasse",
                        "password", "SicheresPasswort123", "roles", new String[]{"KASSIERER"},
                        "overrides", Map.of("sales.create", false, "stock.book", true)))))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        assertFalse(json.readTree(created.getResponse().getContentAsString()).has("passwort_hash"));

        // Startpasswort muss zuerst gewechselt werden.
        mvc.perform(get("/api/produkte").header("Authorization", basic(username, "SicheresPasswort123")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/account/password").header("Authorization", basic(username, "SicheresPasswort123"))
                .header("X-Kassensystem-Client", "JavaFX").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"SicheresPasswort123\",\"newPassword\":\"NeuesPasswort123\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/produkte").header("Authorization", basic(username, "NeuesPasswort123")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/kasse/abschluss").header("Authorization", basic(username, "NeuesPasswort123"))
                .header("X-Kassensystem-Client", "JavaFX").contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionen\":[]}"))
                .andExpect(status().isForbidden());

        MockHttpSession userSession = login(username, "NeuesPasswort123");
        mvc.perform(patch("/api/admin/users/{id}", id).session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/session").session(userSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/session").header("Authorization", basic(username, "NeuesPasswort123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void letzterAdminUndCsrfSindGeschuetzt() throws Exception {
        MockHttpSession admin = login("admin", "1234");
        long id = accounts.findByLogin("admin").id();
        mvc.perform(patch("/api/admin/users/{id}", id).session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/users/{id}", id).session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"overrides\":{\"users.manage\":false}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users").session(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
        admin.removeAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        MvcResult sessionResponse = mvc.perform(get("/api/session").session(admin))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(sessionResponse.getResponse().getContentAsString()).path("csrfToken").asText();
        assertEquals(((org.springframework.security.web.csrf.CsrfToken) admin.getAttribute(
                "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN")).getToken(), token);
        assertEquals("X-CSRF-TOKEN", ((org.springframework.security.web.csrf.CsrfToken) admin.getAttribute(
                "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN")).getHeaderName());
        mvc.perform(post("/api/admin/users").session(admin)
                .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users").session(login("lagerist", "1234")))
                .andExpect(status().isForbidden());
    }

    @Test
    void browserTicketIstEinmaligUndPasswortResetBeendetSitzung() throws Exception {
        MockHttpSession admin = login("admin", "1234");
        MvcResult issued = mvc.perform(post("/api/auth/browser-ticket").header("Authorization", basic("lagerist", "1234"))
                .header("X-Kassensystem-Client", "JavaFX"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(issued.getResponse().getContentAsString()).path("ticket").asText();
        MvcResult scanWeiterleitung = mvc.perform(get("/auth/javafx-login").param("ticket", token)
                .param("scan", "true")).andExpect(status().is3xxRedirection()).andReturn();
        assertTrue(scanWeiterleitung.getResponse().getRedirectedUrl().contains("scan=1"));
        mvc.perform(get("/auth/javafx-login").param("ticket", token)).andExpect(status().isForbidden());

        String username = unique();
        MvcResult created = mvc.perform(post("/api/admin/users").session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("username", username, "displayName", "Test",
                        "password", "ErstesPasswort123", "roles", new String[]{"LAGERIST"}, "overrides", Map.of()))))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(post("/api/account/password").header("Authorization", basic(username, "ErstesPasswort123"))
                .header("X-Kassensystem-Client", "JavaFX").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"ErstesPasswort123\",\"newPassword\":\"ZweitesPasswort123\"}"))
                .andExpect(status().isNoContent());
        MockHttpSession oldSession = login(username, "ZweitesPasswort123");
        mvc.perform(post("/api/admin/users/{id}/password-reset", id).session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"DrittesPasswort123\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/session").session(oldSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/session").header("Authorization", basic(username, "ZweitesPasswort123")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/session").header("Authorization", basic(username, "DrittesPasswort123")))
                .andExpect(status().isOk());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/login").with(csrf()).param("username", username).param("password", password))
                .andExpect(status().is3xxRedirection()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private String unique() { return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 15); }
}
