package de.mmbbs.kassensystem.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "kassensystem.db.path=target/test-kassensystem-security.db",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.de.mmbbs=WARN",
        "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate=WARN",
        "logging.level.com.zaxxer=WARN",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    private static final String SSO_SECRET = "kassensystem-dev-secret-2026";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminSiehtProduktverwaltung() throws Exception {
        MockHttpSession session = login("admin");

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"ADMIN\"")))
                .andExpect(content().string(containsString("\"manageProducts\":true")))
                .andExpect(content().string(containsString("\"bookStock\":true")));
    }

    @Test
    void lageristDarfWarenzugangAberKeineProduktpflege() throws Exception {
        MockHttpSession session = login("lagerist");

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"LAGERIST\"")))
                .andExpect(content().string(containsString("\"manageProducts\":false")))
                .andExpect(content().string(containsString("\"bookStock\":true")));

        mockMvc.perform(post("/api/produkte")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        MvcResult warenzugang = mockMvc.perform(post("/api/produkte/999999/warenzugang")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menge\":1}"))
                .andReturn();

        assertNotEquals(403, warenzugang.getResponse().getStatus());
    }

    @Test
    void kassiererBleibtAufKasseBeschraenkt() throws Exception {
        MockHttpSession session = login("kassierer");

        mockMvc.perform(get("/admin/").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void javaFxSsoErzeugtWebSessionOhneZweitesPasswort() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/javafx-login")
                        .param("ticket", ticket("admin", "ADMIN"))
                        .param("theme", "dark"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/index.html?theme=dark"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"ADMIN\"")));
    }

    private MockHttpSession login(String benutzername) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", benutzername)
                        .param("password", "1234"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String ticket(String benutzername, String rolle) throws Exception {
        long expiresAt = Instant.now().plusSeconds(120).getEpochSecond();
        String payload = benutzername + ":" + rolle + ":" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + signiere(payload);
    }

    private String signiere(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SSO_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
