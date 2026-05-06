import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;


@route("/user")
interface UserRoutes {
  @tag("User")
  @summary("Обновить информацию о пользователе")
  @patch
  @route("/me")
  updateUserInfo(
    @body request: UpdateUserRequest
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<User>;
  } | {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  } | {
    @statusCode statusCode: 401;
    @body body: ErrorResponse;
  } | {
    @statusCode statusCode: 409;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Информация о пользователе")
  @get
  @route("/me")
  info(
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<User>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Смена пароля")
  @post
  @route("/password/change")
  changePassword(
    @body request: ChangePasswordRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401 | 403;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Установка пароля (если ещё не установлен)")
  @post
  @route("/password/set")
  setPassword(
    @body request: SetPasswordRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Отправка письма для сброса пароля")
  @post
  @route("/password/send-reset")
  sendResetPassword(
    @body request: SendResetPasswordRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Сброс пароля")
  @post
  @route("/password/reset")
  resetPassword(
    @body request: ResetPasswordRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Подтверждение email")
  @patch
  @route("/email/verify")
  verifyEmail(
    @body request: VerifyEmailRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Повторная отправка письма подтверждения email")
  @post
  @route("/email/verify/resend")
  resendEmailVerification(): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401 | 409;
    @body body: ErrorResponse;
  };

  @tag("User")
  @summary("Получить статус безопасности пользователя")
  @get
  @route("/security-status")
  getSecurityStatus(): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<SecurityStatusDto>;
  } |
  {
    @statusCode statusCode: 401;
    @body body: ErrorResponse;
  };
}