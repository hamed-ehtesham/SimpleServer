import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

/**
 * Created by Hamed on 9/23/2015.
 */
public class MessagingHandler extends ServerHandler {

    public MessagingHandler(String ADDRESS, int PORT, long TIMEOUT) {
        super(ADDRESS, PORT, TIMEOUT);
        init();
    }

    public static void main(String[] args) {
        MessagingHandler messageSyncHandler = new MessagingHandler("localhost", 8516, 10000);
        Thread thread = new Thread(messageSyncHandler);
        thread.start();
    }

    @Override
    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Prepare key for send server's public key to client
        SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_READ);
        writeKey.attach(ConnectionSteps.Messaging.IDENTIFICATION);
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        switch ((ConnectionSteps.Messaging) key.attachment()) {
            case SYNC_RESPOND: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                IdentificationInfo identificationInfo = (IdentificationInfo) attachment.get(0);
                RespondInfo respond = new RespondInfo();
                //see if there is new messages in db
                String[] sessionInfo = DBHelper.getSessionInfo(identificationInfo.getSessionID());
                String symmetricKey = sessionInfo[1];
                if (symmetricKey == null || symmetricKey == "") {
                    respond.setSucceed(false);
                    respond.setMessage("there is not such a symmetric key");
                } else {
                    respond.setSucceed(true);
                }
                ByteBuffer buffer = XMLUtil.marshal(respond);
                channel.write(buffer);
//                System.out.println(new String(buffer.array()));
                if (respond.getSucceed()) {
                    ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Messaging.MESSAGE_INFO,symmetricKey);
                } else
                    key.cancel();
                break;
            }
            case MESSAGE_RESPOND: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                String symmetricKey = (String) attachment.get(0);
                MessageInfo messageInfo = (MessageInfo) attachment.get(1);
                //write message in DB
                RespondInfo respondInfo = DBHelper.insertMessage(messageInfo);
                ByteBuffer buffer = XMLUtil.marshal(respondInfo);
                buffer = ChannelHelper.encrypt(buffer, symmetricKey);
                channel.write(buffer);
                key.cancel();
                break;
            }
        }
    }

    @Override
    protected void read(SelectionKey key) throws IOException {
        byte[] data = ChannelHelper.read(key);
        switch ((ConnectionSteps.Messaging) key.attachment()) {
            case IDENTIFICATION: {
                IdentificationInfo identificationInfo = XMLUtil.unmarshal(IdentificationInfo.class, data);
                System.out.println(identificationInfo);
                ConnectionSteps.attach(key, SelectionKey.OP_WRITE, ConnectionSteps.Messaging.SYNC_RESPOND, identificationInfo);
                break;
            }
            case MESSAGE_INFO: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                String symmetricKey = (String) attachment.get(0);
                data = ChannelHelper.decrypt(data, symmetricKey);
                MessageInfo messageInfo = XMLUtil.unmarshal(MessageInfo.class, data);
                System.out.println(messageInfo);
                ConnectionSteps.attach(key, SelectionKey.OP_WRITE, ConnectionSteps.Messaging.MESSAGE_RESPOND,symmetricKey,messageInfo);
                break;
            }
        }
    }
}
