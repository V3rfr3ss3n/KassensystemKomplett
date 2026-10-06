package de.mmbbs.kassensystem.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"kassensystem.db.path=target/test-scan-codes.db", "kassensystem.auth.demo=true", "debug=false", "logging.level.root=WARN", "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
class ScanCodeIntegrationTest {
    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void automatischeUndManuelleCodesBleibenEindeutig() throws Exception {
        JsonNode automatisch = anlegen("{\"name\":\"Scan A\",\"preis\":1,\"lagerbestand\":2}");
        int id = automatisch.path("id").asInt();
        assertEquals("KS-P-%06d".formatted(id), automatisch.path("scanCode").asText());
        String code = "400638133" + id;
        JsonNode manuell = anlegen("{\"name\":\"Scan B\",\"preis\":2,\"lagerbestand\":2,\"scanCode\":\"  " + code + "  \"}");
        assertEquals(code, manuell.path("scanCode").asText());
        mvc.perform(put("/api/produkte/{id}", id).with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Scan A2\",\"preis\":1,\"lagerbestand\":2}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/produkte/{id}", id).with(httpBasic("admin", "1234")))
                .andExpect(status().isOk());
        JsonNode gespeichert = mapper.readTree(mvc.perform(get("/api/produkte/{id}", id)
                .with(httpBasic("admin", "1234"))).andReturn().getResponse().getContentAsString());
        assertEquals(automatisch.path("scanCode").asText(), gespeichert.path("scanCode").asText());
        mvc.perform(post("/api/produkte").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Doppelt\",\"preis\":1,\"lagerbestand\":1,\"scanCode\":\"" + code + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/produkte").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Zeilenbruch\",\"preis\":1,\"lagerbestand\":1,\"scanCode\":\"a\\nb\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void etikettenSindPdfUndNurFuerProduktverwalter() throws Exception {
        JsonNode produkt = anlegen("{\"name\":\"Etikett\",\"preis\":1,\"lagerbestand\":2}");
        String body = "{\"produktIds\":[" + produkt.path("id").asInt() + "]}";
        MvcResult pdf = mvc.perform(post("/api/produkte/etiketten").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        assertTrue(pdf.getResponse().getContentType().startsWith("application/pdf"));
        assertEquals("%PDF", new String(pdf.getResponse().getContentAsByteArray(), 0, 4));
        mvc.perform(post("/api/produkte/etiketten").with(httpBasic("lagerist", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/produkte/etiketten").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"produktIds\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/produkte/etiketten").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"produktIds\":[999999]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void handyBildLiestQrCodeMitProduktLeserecht() throws Exception {
        byte[] bild = qrBild("KS-P-000123");
        mvc.perform(post("/api/produkte/scan-bild").with(httpBasic("lagerist", "1234")).with(csrf())
                        .contentType(MediaType.IMAGE_PNG).content(bild))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.scanCode")
                        .value("KS-P-000123"));
        mvc.perform(post("/api/produkte/scan-bild").with(httpBasic("lagerist", "1234"))
                        .contentType(MediaType.IMAGE_PNG).content(bild))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/produkte/scan-bild").with(csrf())
                        .contentType(MediaType.IMAGE_PNG).content(bild))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/produkte/scan-bild").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.IMAGE_PNG).content(new byte[1_048_577]))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/produkte/scan-bild").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.IMAGE_PNG).content("kein Bild"))
                .andExpect(status().isBadRequest());
    }

    private byte[] qrBild(String code) throws Exception {
        var matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage bild = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) bild.setRGB(x, y, matrix.get(x, y) ? 0 : 0xffffff);
        }
        ByteArrayOutputStream ausgabe = new ByteArrayOutputStream();
        ImageIO.write(bild, "png", ausgabe);
        return ausgabe.toByteArray();
    }

    private JsonNode anlegen(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/produkte").with(httpBasic("admin", "1234")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
