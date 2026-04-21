import "./user.tsp";

namespace AuthService;

@doc("Запрос на обновление ролей пользователя")
model RoleUpdateRequest {
  @doc("Новый набор ролей")
  @minItems(1)
  userRoles: Roles[];
}