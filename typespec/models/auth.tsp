
namespace AuthService;

model RegisterRequest {
    @minLength(1)
    @maxLength(50)
    username: string;

    @minLength(3)
    @maxLength(100)
    email: string;

    @minLength(6)
    @maxLength(50)
    password: string;
}

model AuthResponseModel {
    accessToken: string;
    refreshToken: string;
}