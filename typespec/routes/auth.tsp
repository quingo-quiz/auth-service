import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;

@route("/")
interface AuthRoutes {
  @summary("Регистрация")
  @post
  @route("/register")
  register(
    @header("Auth-Strategy") strategy?: string = "cookie",
    @body request: RegisterRequest,
  ): {
    @statusCode statusCode: 200;
    @header("Set-Cookie") setCookie?: string[];
    @body body: SuccessResponse<AuthResponseModel>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @summary("Вход")
  @post
  @route("/auth")
  login(
    @header("Auth-Strategy") strategy?: string = "cookie",
    @body request: AuthRequest,
  ): {
    @statusCode statusCode: 200;
    @header("Set-Cookie") setCookie?: string[];
    @body body: SuccessResponse<AuthResponseModel>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @summary("Обновление токена")
  @post
  @route("/refresh")
  refresh(
    @header("Auth-Strategy") strategy?: string = "cookie",
    @header("Cookie") cookie?: string,
    @body request?: RefreshTokenRequest,
  ): {
    @statusCode statusCode: 200;
    @header("Set-Cookie") setCookie?: string[];
    @body body: SuccessResponse<AuthResponseModel>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @summary("Выход из системы")
  @post
  @route("/logout")
  logout(
    @header("Auth-Strategy") strategy?: string = "cookie",
    @header("Cookie") cookie?: string,
    @body request?: RefreshTokenRequest,
  ): {
    @statusCode statusCode: 200;
    @header("Set-Cookie") setCookie?: string[];
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @summary("Выход со всех устройств")
  @post
  @route("/logout/all")
  logoutAll(
    @header("Auth-Strategy") strategy?: string = "cookie",
    @header("Cookie") cookie?: string,
    @body request?: RefreshTokenRequest,
  ):
  {
    @statusCode statusCode: 200;
    @header("Set-Cookie") setCookie?: string[];
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  }
}