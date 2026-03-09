package tech.arhr.quingo.auth_service.api.rest.controllers;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

class AuthControllerTest extends BaseRestApiTest {

    @Test
    void register_ValidRequest_ReturnsCookies() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "username", "testName",
                        "password", "testPassword",
                        "email", "test@gmail.com"
                ))
                .when()
                .post("/register")
                .then()
                .statusCode(200)
                .cookie("access_token")
                .cookie("refresh_token");
    }

    @Test
    void register_DuplicateEmail_ReturnsConflict() {
        TestUser user = createTestUser();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "username", "otherusername",
                        "password", "testpassword",
                        "email", user.email()
                ))
                .when()
                .post("/register")
                .then()
                .statusCode(409);
    }

    @Test
    void register_MissingRequiredFields_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "testName"))
                .when()
                .post("/register")
                .then()
                .statusCode(400);
    }

    @Test
    void login_ValidCredentials_ReturnsCookies() {
        TestUser user = createTestUser();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", user.email(),
                        "password", user.password()
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .cookie("access_token")
                .cookie("refresh_token");
    }

    @Test
    void login_ValidCredentials_CookiesAreHttpOnly() {
        TestUser user = createTestUser();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", user.email(),
                        "password", user.password()
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .header("Set-Cookie", containsString("HttpOnly"));
    }

    @Test
    void login_InvalidPassword_ReturnsBadRequest() {
        TestUser user = createTestUser();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", user.email(),
                        "password", "password"
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(400);
    }

    @Test
    void login_InvalidEmail_ReturnsBadRequest() {
        TestUser user = createTestUser();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "invalid@ml.com",
                        "password", user.password()
                ))
                .when()
                .post("/auth")
                .then()
                .statusCode(400);
    }

    @Test
    void refresh_ValidToken_ReturnsCookies() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/refresh")
                .then()
                .statusCode(200)
                .cookie("access_token")
                .cookie("refresh_token");
    }

    @Test
    void refresh_OldTokenIsRevokedAfterRefresh() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/refresh")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    void refresh_NoToken_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    void logout_ValidToken_ReturnsOk() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout")
                .then()
                .statusCode(200);
    }

    @Test
    void logout_ValidToken_ClearsCookies() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout")
                .then()
                .statusCode(200)
                .cookie("access_token")
                .cookie("refresh_token");
    }

    @Test
    void logout_RevokedToken_ReturnsUnauthorized() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout")
                .then()
                .statusCode(401);
    }

    @Test
    void logout_NoToken_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/logout")
                .then()
                .statusCode(400);
    }

    @Test
    void logoutAll_ValidToken_ReturnsOk() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout/all")
                .then()
                .statusCode(200);
    }

    @Test
    void logoutAll_RevokedToken_ReturnsUnauthorized() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout/all")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout/all")
                .then()
                .statusCode(401);
    }

    @Test
    void logoutAll_NoToken_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/logout/all")
                .then()
                .statusCode(400);
    }

    @Test
    void logout_WithAccessToken_BlocksAccessToken() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .cookie("access_token", tokens.accessToken())
                .when()
                .post("/logout")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie("access_token", tokens.accessToken())
                .when()
                .post("/internal/authorize")
                .then()
                .statusCode(401);
    }

    @Test
    void logout_WithoutAccessToken_ReturnsOk() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout")
                .then()
                .statusCode(200);
    }

    @Test
    void logoutAll_BlocksAccessToken() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/logout/all")
                .then()
                .statusCode(200);

        given()
                .cookie("access_token", tokens.accessToken())
                .when()
                .post("/internal/authorize")
                .then()
                .statusCode(401);
    }

    @Test
    void logoutAll_BlocksRefreshTokenOnAllDevices() {
        TestUser user = createTestUser();
        Tokens firstDevice = login(user.email(), user.password());
        Tokens secondDevice = login(user.email(), user.password());

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", firstDevice.refreshToken())
                .when()
                .post("/logout/all")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", secondDevice.refreshToken())
                .when()
                .post("/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    void register_JsonStrategy_ReturnsTokensInBody() {
        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of(
                        "username", "jsonUser",
                        "password", "testPassword",
                        "email", "jsonuser@gmail.com"
                ))
                .when()
                .post("/register")
                .then()
                .statusCode(200)
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue());
    }

    @Test
    void login_JsonStrategy_ReturnsTokensInBody() {
        TestUser user = createTestUser();

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
                .statusCode(200)
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue());
    }

    @Test
    void refresh_JsonStrategy_ValidTokenInBody_ReturnsTokensInBody() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/refresh")
                .then()
                .statusCode(200)
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue());
    }

    @Test
    void refresh_JsonStrategy_OldTokenRevokedAfterRefresh() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/refresh")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    void refresh_JsonStrategy_MissingTokenInBody_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of())
                .when()
                .post("/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    void refresh_JsonStrategy_TokenInCookieIgnored_ReturnsBadRequest() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .cookie("refresh_token", tokens.refreshToken())
                .when()
                .post("/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    void logout_JsonStrategy_ValidTokenInBody_ReturnsOk() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/logout")
                .then()
                .statusCode(200);
    }

    @Test
    void logout_JsonStrategy_RevokedToken_ReturnsUnauthorized() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/logout")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/logout")
                .then()
                .statusCode(401);
    }

    @Test
    void logout_JsonStrategy_MissingTokenInBody_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of())
                .when()
                .post("/logout")
                .then()
                .statusCode(400);
    }

    @Test
    void logoutAll_JsonStrategy_ValidTokenInBody_ReturnsOk() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/logout/all")
                .then()
                .statusCode(200);
    }

    @Test
    void logoutAll_JsonStrategy_RevokedToken_ReturnsUnauthorized() {
        Tokens tokens = createUserAndGetTokens();

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/logout/all")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of("refresh_token", tokens.refreshToken()))
                .when()
                .post("/logout/all")
                .then()
                .statusCode(401);
    }

    @Test
    void logoutAll_JsonStrategy_MissingTokenInBody_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "json")
                .body(Map.of())
                .when()
                .post("/logout/all")
                .then()
                .statusCode(400);
    }

    @Test
    void register_InvalidStrategy_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .header("Auth-Strategy", "invalid")
                .body(Map.of(
                        "username", "testName",
                        "password", "testPassword",
                        "email", "strategytest@gmail.com"
                ))
                .when()
                .post("/register")
                .then()
                .statusCode(400);
    }
}