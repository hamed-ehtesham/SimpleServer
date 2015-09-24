import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
public class CreateGroupRequest {

    public static void main(String[] args) {

        GroupInfo customer = new GroupInfo();
        customer.setId(100);
        customer.setName("mkyong");
        List<GroupMemberInfo> test = new ArrayList<GroupMemberInfo>();
        test.add(new GroupMemberInfo("ali1"));
        customer.setMembers(test);
        customer.setOwner("mohammad");

        try {

            File file = new File("D:\\file.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(GroupInfo.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(customer, file);
            jaxbMarshaller.marshal(customer, System.out);

        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

}
