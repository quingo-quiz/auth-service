import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;


@route("/user")
interface UserRoutes {
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
}