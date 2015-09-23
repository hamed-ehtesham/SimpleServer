import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Hamed on 9/20/2015.
 */
@XmlRootElement
public class LoginRespondInfo extends XMLInfo {
    Boolean succeed;
    String message;
    String sessionID;

    public Boolean getSucceed() {
        return succeed;
    }

    @XmlElement
    public void setSucceed(Boolean succeed) {
        this.succeed = succeed;
    }

    public String getMessage() {
        return message;
    }

    @XmlElement
    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionID() {
        return sessionID;
    }

    @XmlElement
    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    @Override
    public String toString() {
        return "RegistrationRespondInfo{" +
                "succeed=" + succeed +
                ", message='" + message + '\'' +
                ", sessionID='" + sessionID + '\'' +
                '}';
    }
}
