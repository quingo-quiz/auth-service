import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;


@route("/verify")
interface VerifyRoutes {
  @summary("Подтвердить почту")
  @get
  info(
      @path token: string
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<boolean>;
  };
}