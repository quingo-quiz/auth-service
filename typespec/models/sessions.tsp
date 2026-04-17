namespace AuthService;

@doc("Информация о сессии")
model SessionModel {
  @doc("ID сессии")
  sessionId: uuid;

  @doc("Время входа")
  issuedAt: utcDateTime;
}
