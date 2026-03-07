package tech.arhr.quingo.auth_service.api.rest.controllers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import tech.arhr.quingo.auth_service.IntegrationTestBase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;


public abstract class BaseRestApiTest extends IntegrationTestBase {

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = super.port;
        RestAssured.basePath = "/auth";
    }

    protected TestUser createTestUser() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        TestUser user = new TestUser(
                "user_" + uniqueId,
                "pass_" + uniqueId,
                "email_" + uniqueId + "@test.com"
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", user.username);
        requestBody.put("password", user.password);
        requestBody.put("email", user.email);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/register")
                .then()
                .statusCode(200);

        return user;
    }

    protected Tokens createUserAndGetTokens() {
        TestUser user = createTestUser();
        return login(user.email, user.password);
    }

    protected Tokens login(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return new Tokens(
                response.cookie("access_token"),
                response.cookie("refresh_token")
        );
    }

    protected record TestUser(String username, String password, String email) {}
    protected record Tokens(String accessToken, String refreshToken) {}
}
