package tech.arhr.quingo.auth_service.utils;


import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class Hasher {
    private static final int COST = 12;
    public String hash(String rawString) {
        return BCrypt.hashpw(rawString, BCrypt.gensalt(COST));
    }

    public boolean verify(String rawString, String hash) {
        return BCrypt.checkpw(rawString, hash);
    }
}
