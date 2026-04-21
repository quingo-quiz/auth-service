import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;

@route("/mfa")
interface MfaRoutes {
  @tag("MFA")
  @summary("Инициализация включения OTP в рамках MFA")
  @post
  @route("/otp/connect")
  otpConnect(): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<OtpConnectDto>;
  } |
  {
    @statusCode statusCode: 401;
    @body body: ErrorResponse;
  };

  @tag("MFA")
  @summary("Подтверждение включения OTP в рамках MFA")
  @post
  @route("/otp/connect/confirm")
  otpConnectConfirm(
    @body request: OtpVerifyRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401;
    @body body: ErrorResponse;
  };
}
