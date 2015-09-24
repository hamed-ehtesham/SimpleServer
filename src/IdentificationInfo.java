import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Hamed on 9/24/2015.
 */
@XmlRootElement
public class IdentificationInfo extends XMLInfo {
    String email;
    String sessionID;

    public String getEmail() {
        return email;
    }

    @XmlElement
    public void setEmail(String email) {
        this.email = email;
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
        return "IdentificationInfo{" +
                "email='" + email + '\'' +
                ", sessionID='" + sessionID + '\'' +
                '}';
    }
}
