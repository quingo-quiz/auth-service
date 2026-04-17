namespace AuthService;

@doc ("Пользователь")
model User {
  id: uuid;
  username: string;
  email: string;
  roles: Roles[];
  emailVerified: boolean;
  accountStatus: AccountStatus;
}

@doc("Запрос на смену пароля")
model ChangePasswordRequest {
  @doc("Текущий пароль")
  @minLength(6)
  @maxLength(50)
  oldPassword: string;

  @doc("Новый пароль")
  @minLength(6)
  @maxLength(50)
  newPassword: string;
}


enum Roles{
    USER,
    ADMIN
}

enum AccountStatus{
    ACTIVE,
    BLOCKED,
    DELETED
}

@format("uuid")
scalar uuid extends string;