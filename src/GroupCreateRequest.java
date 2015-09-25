import javax.crypto.SealedObject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
public class GroupCreateRequest {

    private String groupName;
    private String session;
    private List<GroupMemberInfo> members = new ArrayList<GroupMemberInfo>();

    public GroupCreateRequest(String groupName, List<GroupMemberInfo> members, String session) {
        this.groupName = groupName;
        this.session = session;
        this.members = members;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<GroupMemberInfo> getMembers() {
        return this.members;
    }

    public void setMembers(List<GroupMemberInfo> members) {
        this.members = members;
    }

    public String getSession() {
        return this.session;
    }

    public void setSession(String session) {
        this.session = session;
    }

//    public LoginRespondInfo getRespond() {
//        return respond;
//    }
//
//    public void setRespond(LoginRespondInfo respond) {
//        this.respond = respond;
//    }

    public static void main(String[] args) {
//        GroupCreateRequest request = new GroupCreateRequest("ali_jaf@gmail.com", "i,d[");
//        request.request();
    }

    class request extends ClientHandler {

        public request(String ADDRESS, int PORT) {
            super(ADDRESS, PORT);
        }

        @Override
        protected void connect(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            channel.configureBlocking(false);

            SelectionKey readKey = channel.register(selector, SelectionKey.OP_WRITE);
            readKey.attach(ConnectionSteps.GroupCreate.GROUP_CREATE_INFO);
        }

        @Override
        protected void read(SelectionKey key) throws IOException {
            ByteArrayOutputStream bos = ChannelHelper.read(key);
            byte[] data = bos.toByteArray();
            switch ((ConnectionSteps.GroupCreate) key.attachment()) {
                case GROUP_INFO: {
                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                    data = aesEncryptionUtil.decrypt(data);
                    GroupInfo respondInfo = XMLUtil.unmarshal(GroupInfo.class , data);

                    if (respondInfo != null && respondInfo.getSucceed()) {
                        // TODO: Add Group to Group list as owner in UI
                    }

                    key.cancel();
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        @Override
        protected void write(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();

            switch ((ConnectionSteps.GroupCreate) key.attachment()) {
                case GROUP_CREATE_INFO: {
                    GroupCreateInfo requestInfo = new GroupCreateInfo();
                    requestInfo.setSession(session);
                    requestInfo.setName(groupName);
                    requestInfo.setMembers(members);
                    ByteBuffer buffer = XMLUtil.marshal(requestInfo);
                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                    buffer = aesEncryptionUtil.encrypt(buffer);
                    channel.write(buffer);
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.GroupCreate.GROUP_INFO);
                    break;
                }
            }
        }

    }

    public void request() {
        Thread thread = new Thread(new request("localhost", 8513));
        thread.start();
    }

}
