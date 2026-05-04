namespace AuthService;

@doc ("Пользователь")
model User {
  id: uuid;
  username: string;
  email: string;
  roles: Roles[];
  emailVerified: boolean;
  mfaEnabled: boolean;
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

@doc("Статус безопасности пользователя")
model SecurityStatusDto {
  @doc("ID пользователя")
  userId: uuid;

  @doc("Email пользователя")
  email: string;

  @doc("Подтвержден ли email")
  emailVerified: boolean;

  @doc("Установлен ли пароль")
  passwordSet: boolean;

  @doc("Включена ли MFA")
  mfaEnabled: boolean;

  @doc("Типы MFA")
  mfaTypes: MfaType[];

  @doc("Роли пользователя")
  roles: Roles[];

  @doc("Связанные OAuth2 провайдеры")
  linkedProviders: OAuth2Provider[];

  @doc("Статус аккаунта")
  accountStatus: AccountStatus;
}

enum MfaType {
  OTP
}

enum OAuth2Provider {
  GOOGLE,
  GITHUB
}