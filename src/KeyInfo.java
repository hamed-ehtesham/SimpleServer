import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Hamed on 9/20/2015.
 */
@XmlRootElement
public class KeyInfo extends XMLInfo {
    String key;

    public String getKey() {
        return key;
    }

    @XmlElement
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "KeyInfo{" +
                "key='" + key + '\'' +
                '}';
    }
}
