import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;


@route("/verify")
interface VerifyRoutes {
  @tag("Auth")
  @summary("Подтвердить почту")
  @get
  verify(
      @path token: string
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<boolean>;
  };
}