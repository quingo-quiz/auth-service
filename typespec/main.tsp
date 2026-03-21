import "@typespec/http";
import "./models";
import "./routes";

using Http;

@service(#{ title: "Auth Service" })
@server("https://api.quiz-platform.com/auth", "Production server")
namespace AuthService;