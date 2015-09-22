import java.math.BigInteger;
import java.net.SocketAddress;
import java.security.SecureRandom;

/**
 * Created by Hamed on 9/22/2015.
 */
public final class SymmetricUtil {
    private static SecureRandom random = new SecureRandom();

    public static String nextSymmetricKey() {
        return new BigInteger(130, random).toString(32);
    }

    public static String getSessionID(String username, SocketAddress ip, String secretKey) {
        StringBuilder sessionID = new StringBuilder(100);
        sessionID.append(System.currentTimeMillis());
        sessionID.append('_');
        sessionID.append(username);
        sessionID.append('_');
        sessionID.append(ip);
        AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(secretKey);
        aesEncryptionUtil.encrypt(sessionID.toString());
        return aesEncryptionUtil.getEncryptedString();
    }
}
