import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mohammad Amin on 23/09/2015.
 */
public class ValidationUtil {
    private static final Pattern emailPattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

    public static boolean validateEmail(String email) {
        Matcher matcher = emailPattern.matcher(email);
        if (matcher.matches())
            return true;
        else
            return false;
    }
}
