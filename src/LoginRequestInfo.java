import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Hamed on 9/20/2015.
 */
@XmlRootElement
public class LoginRequestInfo extends XMLInfo {
    String email;
    String password;
    String ip;

    public String getEmail() {
        return email;
    }

    @XmlElement
    public void setEmail(String email) {
        this.email = email;
    }

    public String getIP() {
        return ip;
    }

    @XmlElement
    public void setIP(String ip) {
        this.ip = ip;
    }


    public String getPassword() {
        return password;
    }

    @XmlElement
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "RegistrationRequestInfo{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
