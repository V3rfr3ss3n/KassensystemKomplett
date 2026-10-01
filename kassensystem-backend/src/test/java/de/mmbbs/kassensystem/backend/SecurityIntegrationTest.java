package de.mmbbs.kassensystem.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private JdbcTemplate jdbc;

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

        mockMvc.perform(put("/api/produkte/999999")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/produkte/999999").session(session))
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
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/produkte").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/bons").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/api/produkte").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/produkte/1/warenzugang").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"menge\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void desktopClientKannBasicAuthNutzen() throws Exception {
        String credentials = Base64.getEncoder().encodeToString("kassierer:1234".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/api/session").header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("kassierer")));
        mockMvc.perform(get("/api/session").header("Authorization", "Basic " +
                        Base64.getEncoder().encodeToString("kassierer:falsch".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/kasse/abschluss").header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_JSON).content("{\"positionen\":[]}"))
                .andExpect(status().isBadRequest());
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

    @Test
    void produktApiErstelltListetAktualisiertUndLoeschtProdukte() throws Exception {
        MockHttpSession session = login("admin");
        String daten = """
                {"name":"API Testprodukt","preis":2.49,"lagerbestand":3,"einheit":"STUECK","steuerSatz":19}
                """;

        MvcResult erstellt = mockMvc.perform(post("/api/produkte")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(daten))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("API Testprodukt")))
                .andReturn();
        String body = erstellt.getResponse().getContentAsString();
        int id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(body).get("id").asInt();

        mockMvc.perform(get("/api/produkte").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("API Testprodukt")));

        mockMvc.perform(post("/api/produkte/{id}/warenzugang", id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menge\":2.5}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5.5")));

        mockMvc.perform(put("/api/produkte/{id}", id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aktualisiertes Produkt\",\"preis\":3.49,\"lagerbestand\":5.5,\"einheit\":\"STUECK\",\"steuerSatz\":19}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aktualisiertes Produkt")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/produkte/{id}", id)
                        .session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/produkte/{id}", id).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void produktApiLehntUnendlicheWerteUndUngueltigeMengenAb() throws Exception {
        MockHttpSession session = login("admin");
        mockMvc.perform(post("/api/produkte")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ungueltig\",\"preis\":\"Infinity\",\"lagerbestand\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/produkte/999999/warenzugang")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menge\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kaufNutztServerpreiseUndHistorischerBonBehaeltSnapshot() throws Exception {
        MockHttpSession admin = login("admin");
        MockHttpSession kassierer = login("kassierer");
        MvcResult erstellt = mockMvc.perform(post("/api/produkte").session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Snapshot Test\",\"preis\":2.5,\"lagerbestand\":4,\"einheit\":\"STUECK\",\"steuerSatz\":7}"))
                .andExpect(status().isCreated()).andReturn();
        int id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(erstellt.getResponse().getContentAsString()).get("id").asInt();

        MvcResult kauf = mockMvc.perform(post("/api/kasse/abschluss").session(kassierer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionen\":[{\"produktId\":" + id + ",\"menge\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"gesamtpreis\":5.0")))
                .andReturn();
        int bonnummer = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(kauf.getResponse().getContentAsString()).get("bonnummer").asInt();
        org.junit.jupiter.api.Assertions.assertEquals(2.0,
                jdbc.queryForObject("SELECT lagerbestand FROM produkte WHERE id = ?", Double.class, id));

        mockMvc.perform(put("/api/produkte/{id}", id).session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"preis\":9,\"lagerbestand\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/bons/{id}", bonnummer).session(kassierer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Snapshot Test")));
        mockMvc.perform(delete("/api/produkte/{id}", id).session(admin)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bons/{id}", bonnummer).session(kassierer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Snapshot Test")));
    }

    @Test
    void fehlgeschlagenerKaufAendertWederBestandNochHistorie() throws Exception {
        MockHttpSession admin = login("admin");
        MockHttpSession kassierer = login("kassierer");
        MvcResult erstellt = mockMvc.perform(post("/api/produkte").session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Rollback Test\",\"preis\":1,\"lagerbestand\":3}"))
                .andExpect(status().isCreated()).andReturn();
        int id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(erstellt.getResponse().getContentAsString()).get("id").asInt();
        int vorher = jdbc.queryForObject("SELECT COUNT(*) FROM bons", Integer.class);

        mockMvc.perform(post("/api/kasse/abschluss").session(kassierer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionen\":[{\"produktId\":" + id + ",\"menge\":1},{\"produktId\":999999,\"menge\":1}]}"))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(3.0,
                jdbc.queryForObject("SELECT lagerbestand FROM produkte WHERE id = ?", Double.class, id));
        org.junit.jupiter.api.Assertions.assertEquals(vorher,
                jdbc.queryForObject("SELECT COUNT(*) FROM bons", Integer.class));
        mockMvc.perform(delete("/api/produkte/{id}", id).session(admin)).andExpect(status().isNoContent());
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
