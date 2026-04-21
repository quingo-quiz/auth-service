import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;

@route("/")
interface AuthRoutes {
  @tag("Auth")
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
    @statusCode statusCode: 400 | 409;
    @body body: ErrorResponse;
  };

  @tag("Auth")
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
    @statusCode statusCode: 403;
    @body body: SuccessResponse<AuthResponseModel>;
  } |
  {
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };

  @tag("Auth")
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
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };

  @tag("Auth")
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
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };

  @tag("Auth")
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
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };

  @tag("OAuth2")
  @summary("Инициализация OAuth2 flow через GitHub")
  @get
  @route("/oauth2/authorization/github")
  oauth2Github(): {
    @statusCode statusCode: 302;
    @header("Location") location: string;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @tag("OAuth2")
  @summary("Инициализация OAuth2 flow через Google")
  @get
  @route("/oauth2/authorization/google")
  oauth2Google(): {
    @statusCode statusCode: 302;
    @header("Location") location: string;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @tag("Tokens")
  @summary("Получить список активных токенов")
  @get
  @route("/tokens")
  listSessions(): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<TokenModel[]>;
  } |
  {
    @statusCode statusCode: 401;
    @body body: ErrorResponse;
  };

  @tag("MFA")
  @summary("Проверка OTP кода в рамках 2FA flow")
  @post
  @route("/mfa/otp/verify")
  otpVerify(
    @header("Auth-Strategy") strategy?: string = "cookie",
    @body request: OtpVerifyRequest,
  ): {
    @statusCode statusCode: 200;
    @header("Set-Cookie") setCookie?: string[];
    @body body: SuccessResponse<AuthResponseModel>;
  } |
  {
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };
}