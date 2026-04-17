import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;


@route("/user")
interface UserRoutes {
  @tag("User")
  @summary("Информация о пользователе")
  @get
  @route("/info")
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
  @route("/change-password")
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
}