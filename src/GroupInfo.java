import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
@XmlRootElement
public class GroupInfo extends XMLInfo {
    int id;
    String name;
    String owner;
    Members member;

    public int getId() {
        return this.id;
    }

    @XmlElement
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return this.owner;
    }

    @XmlElement
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Members getMember() {
        return this.member;
    }

    @XmlElement
    public void setMember(Members member) {
        this.member = member;
    }


    @XmlRootElement
    public class Members {
        @XmlList
        List<String> member;

        public List<String> getMembers() {
            return this.member;
        }

        @XmlElement
        public void setMember(String member) {
            this.member.add(member);
        }
    }


}


