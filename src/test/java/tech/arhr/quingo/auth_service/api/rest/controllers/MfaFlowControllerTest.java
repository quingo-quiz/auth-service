package tech.arhr.quingo.auth_service.api.rest.controllers;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class MfaFlowControllerTest extends BaseRestApiTest {

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
