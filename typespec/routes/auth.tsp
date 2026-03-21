import "@typespec/http";

using Http;


namespace AuthService;

@route("/auth")
interface AuthRoutes {

  @summary("Регистрация нового пользователя")
  @post
  @route("/register")
  register(@body request: RegisterRequest): {
    @statusCode statusCode: 200;
    @body body: AuthResponseModel;
  };
}