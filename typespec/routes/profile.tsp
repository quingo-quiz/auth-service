import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;


@route("/profile")
interface ProfileRoutes {
  @tag("Profile")
  @summary("Публичный профиль пользователя по ID")
  @get
  @route("/{id}")
  getProfile(
    @path id: uuid,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<UserProfile>;
  } | {
    @statusCode statusCode: 404;
    @body body: ErrorResponse;
  };

  @tag("Profile")
  @summary("Профили нескольких пользователей по списку ID")
  @post
  @route("/batch")
  getProfiles(
    @body ids: uuid[],
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<UserProfile[]>;
  };
}
