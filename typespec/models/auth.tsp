
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

model OtpConnectDto {
  secretUri: string;
}

@doc("Информация о токене")
model TokenModel {
  @doc("ID токена")
  tokenId: uuid;

  @doc("Время выдачи")
  issuedAt: utcDateTime;
}
