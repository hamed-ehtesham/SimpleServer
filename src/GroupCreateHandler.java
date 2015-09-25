import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Mohammad Amin on 24/09/2015.
 */
public class GroupCreateHandler extends ServerHandler{
    private String symmetricKey;

    public GroupCreateHandler(String ADDRESS, int PORT, long TIMEOUT) {
        super(ADDRESS, PORT, TIMEOUT);
        init();
    }

    public static void main(String[] args) {
        GroupCreateHandler groupCreateHandler = new GroupCreateHandler("localhost", 8511, 10000);
        Thread thread = new Thread(groupCreateHandler);
        thread.start();
    }

    @Override
    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Prepare key for send server's public key to client
        SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_READ);
        writeKey.attach(ConnectionSteps.GroupCreate.GROUP_CREATE_INFO);
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        switch ((ConnectionSteps.GroupCreate) key.attachment()) {
            case GROUP_INFO: {
                GroupInfo respondInfo = new GroupInfo();
                ConnectionSteps.GroupCreate respond = (ConnectionSteps.GroupCreate) key.attachment();
                try {
                    GroupCreateInfo requestInfo = (GroupCreateInfo) respond.getAttachment();
                    if (requestInfo != null) {
                        respondInfo = DBHelper.createGroup(requestInfo);
                        if(respondInfo.getSucceed()){
                            //TODO : send group info to all members
                        }
                    } else {
                        throw new IOException("GroupCreateInfo not received!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    respondInfo.setSucceed(false);
                }
                AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                ByteBuffer buffer = XMLUtil.marshal(respondInfo);
                channel.write( aesEncryptionUtil.encrypt(buffer));
                key.cancel();
                break;
            }
        }

    }

    @Override
    protected void read(SelectionKey key) throws IOException {
        ByteArrayOutputStream bos = ChannelHelper.read(key);
        byte[] data = bos.toByteArray();
        switch ((ConnectionSteps.GroupCreate) key.attachment()) {
            case GROUP_CREATE_INFO: {
                AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                data = aesEncryptionUtil.decrypt(data);
                GroupCreateInfo requestInfo = XMLUtil.unmarshal(GroupCreateInfo.class, data);
//                System.out.println(requestInfo);
                key.interestOps(SelectionKey.OP_WRITE);
                ConnectionSteps.GroupCreate respond = ConnectionSteps.GroupCreate.GROUP_INFO;
                respond.setAttachment(requestInfo);
                key.attach(respond);
                break;
            }
        }
    }
}
