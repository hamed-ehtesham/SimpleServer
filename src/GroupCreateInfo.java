import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
@XmlRootElement
public class GroupCreateInfo extends XMLInfo {
    String name;
    List<GroupMemberInfo> members = new ArrayList<GroupMemberInfo>();
    String session;


    public String getName() {
        return this.name;
    }

    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public List<GroupMemberInfo> getMembers() {
        return this.members;
    }

    @XmlElement
    public void setMembers(List<GroupMemberInfo> members) {
        this.members = members;
    }

    public String getSession() {
        return this.session;
    }

    @XmlElement
    public void setSession(String session) {
        this.session = session;
    }

}


