package tech.arhr.quingo.auth_service.api.rest.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tech.arhr.quingo.auth_service.data.sql.JpaOutboxRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.OutboxEventEntity;
import tech.arhr.quingo.auth_service.enums.EventType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class VerificationControllerTest extends BaseRestApiTest {

    @Autowired
    private JpaOutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void verifyToken_WithoutAccessToken_ReturnsUnauthorized() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/verify/not-existing-token")
                .then()
                .statusCode(401);
    }

    @Test
    void verifyToken_WithAccessToken_InvalidToken_ReturnsBadRequest() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("access_token", tokens.accessToken())
                .when()
                .get("/verify/not-existing-token")
                .then()
                .statusCode(400)
                .body("data", equalTo(false));
    }

    @Test
    void verifyToken_WithAccessToken_ValidToken_ReturnsOk() {
        TestUser user = createTestUser();
        Tokens tokens = login(user.email(), user.password());
        String verificationToken = findVerificationTokenByEmail(user.email());
        assertThat(verificationToken).isNotBlank();

        given()
                .contentType(ContentType.JSON)
                .cookie("access_token", tokens.accessToken())
                .when()
                .get("/verify/{token}", verificationToken)
                .then()
                .statusCode(200)
                .body("data", equalTo(true));
    }

    private String findVerificationTokenByEmail(String email) {
        return outboxRepository.findAll().stream()
                .filter(event -> event.getEventType() == EventType.VERIFY_EMAIL)
                .map(OutboxEventEntity::getPayload)
                .map(this::safeReadJson)
                .filter(node -> node != null)
                .filter(node -> email.equals(node.path("data").path("email").asText()))
                .map(node -> node.path("data").path("verificationToken").asText())
                .filter(token -> !token.isBlank())
                .findFirst()
                .orElse("");
    }

    private JsonNode safeReadJson(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }
}
