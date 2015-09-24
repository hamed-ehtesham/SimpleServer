import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
@XmlRootElement
public class GroupInfo extends XMLInfo {
    int id;
    String name;
    String owner;
    List<GroupMemberInfo> members = new ArrayList<GroupMemberInfo>();

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

    public List<GroupMemberInfo> getMembers() {
        return this.members;
    }

    @XmlElement
    public void setMembers(List<GroupMemberInfo> members) {
         this.members = members;
    }

}


