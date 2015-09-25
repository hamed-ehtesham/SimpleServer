import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Hamed on 9/20/2015.
 */
@XmlRootElement
public class RespondInfo extends XMLInfo {
    Boolean succeed;
    String message;

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

    @Override
    public String toString() {
        return "RespondInfo{" +
                "succeed=" + succeed +
                ", message='" + message + '\'' +
                '}';
    }
}
