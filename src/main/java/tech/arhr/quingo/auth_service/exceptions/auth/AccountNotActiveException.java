package tech.arhr.quingo.auth_service.exceptions.auth;

public class AccountNotActiveException extends AuthException {
    public AccountNotActiveException(String message) {
        super(message);
    }
    public AccountNotActiveException(){ super("Account is not active"); }
}
