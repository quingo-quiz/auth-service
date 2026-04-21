import "@typespec/http";
import "../models/main.tsp";

using Http;

namespace AuthService;

@route("/admin/users")
interface AdminUserRoutes {
  @tag("Admin")
  @summary("Блокировать пользователя")
  @post
  @route("/{userId}/block")
  blockUser(
    @path userId: uuid,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401 | 403;
    @body body: ErrorResponse;
  };

  @tag("Admin")
  @summary("Разблокировать пользователя")
  @post
  @route("/{userId}/unblock")
  unblockUser(
    @path userId: uuid,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401 | 403;
    @body body: ErrorResponse;
  } ;

  @tag("Admin")
  @summary("Обновить роли пользователя")
  @patch
  @route("/{userId}/roles")
  updateUserRoles(
    @path userId: uuid,
    @body request: RoleUpdateRequest,
  ): {
    @statusCode statusCode: 200;
    @body body: SuccessResponse<null>;
  } |
  {
    @statusCode statusCode: 400 | 401 | 403;
    @body body: ErrorResponse;
  };
}
