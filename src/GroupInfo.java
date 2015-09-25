import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
@XmlRootElement
public class GroupInfo extends XMLInfo {
    String id;
    String name;
    String owner;
    Boolean succeed;
    List<GroupMemberInfo> members = new ArrayList<GroupMemberInfo>();


    public String getId() {
        return this.id;
    }

    @XmlElement
    public void setId(String id) {
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

    public Boolean getSucceed() {
        return this.succeed;
    }

    @XmlElement
    public void setSucceed(Boolean succeed) {
        this.succeed = succeed;
    }

}


