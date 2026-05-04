package tech.arhr.quingo.auth_service.api.rest.controllers;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class UserControllerTest extends BaseRestApiTest {

    @Test
    void info_WithAccessToken_ReturnsCurrentUser() {
        TestUser user = createTestUser();
        Tokens tokens = login(user.email(), user.password());

        given()
                .contentType(ContentType.JSON)
                .cookie("access_token", tokens.accessToken())
                .when()
                .get("/user/info")
                .then()
                .log().all()
                .statusCode(200)
                .body("data.email", equalTo(user.email()))
                .body("data.username", equalTo(user.username()));
    }

    @Test
    void changePassword_ValidRequest_UpdatesCredentials() {
        TestUser user = createTestUser();
        Tokens tokens = login(user.email(), user.password());
        String newPassword = user.password() + "_new";

        given()
                .contentType(ContentType.JSON)
                .cookie("access_token", tokens.accessToken())
                .body(Map.of(
                        "oldPassword", user.password(),
                        "newPassword", newPassword
                ))
                .when()
                .post("/user/change-password")
                .then()
                .log().all()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", user.email(),
                        "password", user.password()
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", user.email(),
                        "password", newPassword
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .cookie("access_token", notNullValue())
                .cookie("refresh_token", notNullValue());
    }
}
