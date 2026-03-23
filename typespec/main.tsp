import "@typespec/http";
import "./models";
import "./routes";

using Http;

@service(#{ title: "Auth Service" })
@server("https://quingo.arhr.tech/api/auth", "Production server")
namespace AuthService;