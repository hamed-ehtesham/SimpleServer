import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Created by Hamed on 9/24/2015.
 */
public class MessageInfoTest {
    public static void main(String[] args) {
        MessageInfo messageInfo = new MessageInfo();
        MessageInfo.Recipient recipient = new MessageInfo.Recipient();
        recipient.setrType(MessageInfo.Recipient.RecipientType.PERSON);
        recipient.setRecipient("hamed");
        messageInfo.setRecipient(recipient);
        messageInfo.setSender("ali");
        messageInfo.setContent("hello this is a test");
        messageInfo.setSession("this is a session");
        messageInfo.setTime(1443106377000L);
        MessageInfo.AttachmentURL attachmentURL = new MessageInfo.AttachmentURL();
        messageInfo.setAttachmentURL(attachmentURL);
        ByteBuffer buffer = XMLUtil.marshal(messageInfo);
        try {
            Channels.newChannel(System.out).write(buffer);
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MessageInfo messageInfo1 = XMLUtil.unmarshal(MessageInfo.class, buffer.array());
        System.out.println(messageInfo1);
        Timestamp timestamp = Timestamp.from(Instant.ofEpochMilli(messageInfo.getTime()));
        System.out.println(timestamp);
    }
}
