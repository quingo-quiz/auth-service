package tech.arhr.quingo.auth_service.utils;


import org.mindrot.jbcrypt.BCrypt;

public class Hasher {
    private static final int COST = 12;
    public static String hash(String rawString) {
        return BCrypt.hashpw(rawString, BCrypt.gensalt(COST));
    }

    public static boolean verify(String rawString, String hash) {
        return BCrypt.checkpw(rawString, hash);
    }
}
