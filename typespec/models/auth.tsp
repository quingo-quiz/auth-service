
namespace AuthService;

model RegisterRequest {
  @minLength(1)
  @maxLength(50)
  username: string;

  @format("email")
  @minLength(3)
  @maxLength(100)
  email: string;

  @minLength(6)
  @maxLength(50)
  password: string;
}

model AuthRequest {
  @format("email")
  @minLength(3)
  @maxLength(100)
  email: string;

  @minLength(6)
  @maxLength(50)
  password: string;
}

model RefreshTokenRequest {
  @encodedName("application/json", "refresh_token")
  refreshToken: string;
}

model AuthResponseModel {
  accessToken?: string;
  refreshToken?: string;
  mfaTempToken?: string;
  mfaRequired: boolean;
}

model OtpVerifyRequest {
  @minLength(1)
  @maxLength(32)
  code: string;

  @minLength(1)
  @maxLength(500)
  mfaTempToken?: string;
}

model OtpDisableRequest {
  @minLength(1)
  @maxLength(32)
  code: string;
}

model OtpConnectDto {
  secretUri: string;
}

@doc("Информация о сессии")
model SessionModel {
  @doc("ID токена")
  tokenId: uuid;

  @doc("ID сессии")
  sessionId: uuid;

  @doc("Время выдачи токена (обновляется при каждом рефреше)")
  issuedAt: utcDateTime;

  @doc("Время первого входа сессии (не меняется при рефрешах) — использовать как «начало сессии»")
  loggedInAt: utcDateTime;

  @doc("Время истечения срока действия")
  expiresAt: utcDateTime;

  @doc("Является ли сессия текущей")
  current: boolean;

  @doc("Браузер")
  browser?: string;

  @doc("Операционная система")
  os?: string;

  @doc("Устройство")
  device?: string;

  @doc("IP адрес")
  ipAddress?: string;
}
