
namespace AuthService;

model RegisterRequest {
  @doc("Тип провайдера, по умолчанию LOCAL")
  provider?: string = "LOCAL";

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
  @doc("Тип провайдера, по умолчанию LOCAL")
  provider?: string = "LOCAL";

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
  accessToken: string;
  refreshToken: string;
}