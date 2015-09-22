import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Hamed on 9/21/2015.
 */
public class XMLUtil {
    public static <T extends XMLInfo> T unmarshal(Class<T> xmlInfo, byte[] data) {
        T xmlInfoObject = null;
        //convert XML to Object
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(xmlInfo);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            xmlInfoObject = (T) jaxbUnmarshaller.unmarshal(bis);
        } catch (JAXBException e) {
//                e.printStackTrace();
        }
        return xmlInfoObject;
    }

    public static <T extends XMLInfo> ByteBuffer marshal(T data) {
        ByteBuffer output = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(data.getClass());
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // write bytes to bos ...
            jaxbMarshaller.marshal(data, bos);
            output = ByteBuffer.wrap(bos.toByteArray());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return output;
    }
}
