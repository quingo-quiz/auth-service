package tech.arhr.quingo.auth_service.api.rest.controllers;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class MfaFlowControllerTest extends BaseRestApiTest {

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    void login_MfaEnabledUser_ReturnsMfaChallenge() {
        TestUser user = createTestUser();
        UserEntity entity = userRepository.findByEmail(user.email()).orElseThrow();
        entity.setMfaEnabled(true);
        userRepository.save(entity);

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of(
                        "email", user.email(),
                        "password", user.password()
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(403)
                .body("data.mfaRequired", equalTo(true))
                .body("data.mfaTempToken", notNullValue());
    }

    @Test
    void otpVerify_InvalidMfaToken_ReturnsUnauthorized() {
        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of(
                        "code", "123456",
                        "mfaTempToken", "invalid-token"
                ))
                .when()
                .post("/mfa/otp/verify")
                .then()
                .statusCode(401);
    }
}
