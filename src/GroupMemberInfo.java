import javax.xml.bind.annotation.XmlElement;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
public class GroupMemberInfo extends XMLInfo {

        String member;

        public GroupMemberInfo(String member){
            setMember(member);
        }

        @XmlElement
        public void setMember(String member) {
            this.member= member;
        }

        public String getMember() {
            return this.member;
        }


}
