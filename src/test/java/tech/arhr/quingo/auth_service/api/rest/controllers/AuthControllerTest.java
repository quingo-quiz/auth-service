package tech.arhr.quingo.auth_service.api.rest.controllers;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

public class AuthControllerTest extends BaseRestApiTest {


    @Test
    void register_ShouldReturnCookies() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testname");
        requestBody.put("password", "testpassword");
        requestBody.put("email", "test@gmail.com");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/register")
                .then()
                .statusCode(200)
                .cookie("access_token")
                .cookie("refresh_token");
    }

    @Test
    void login_ShouldReturnCookies() {
        TestUser user = createTestUser();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", user.password());
        requestBody.put("email", user.email());


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .cookie("access_token")
                .cookie("refresh_token");
    }

    @Test
    void login_InvalidPassword_ShouldReturnBadRequest() {
        TestUser user = createTestUser();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "password");
        requestBody.put("email", user.email());


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth")
                .then()
                .statusCode(400);
    }

    @Test
    void login_InvalidEmail_ShouldReturnBadRequest() {
        TestUser user = createTestUser();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", user.password());
        requestBody.put("email", "invalid@ml.com");


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth")
                .then()
                .statusCode(400);
    }
}
