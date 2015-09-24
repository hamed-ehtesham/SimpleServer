import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/23/2015.
 */
public class MessageSyncHandler extends ServerHandler {
//    private String symmetricKey;

    public MessageSyncHandler(String ADDRESS, int PORT, long TIMEOUT) {
        super(ADDRESS, PORT, TIMEOUT);
        init();
    }

    public static void main(String[] args) {
        MessageSyncHandler messageSyncHandler = new MessageSyncHandler("localhost", 8515, 10000);
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
                ConnectionSteps.Messaging attachment = (ConnectionSteps.Messaging) key.attachment();
                IdentificationInfo identificationInfo = (IdentificationInfo) attachment.getAttachment();
                //see if there is new messages in db
                RespondInfo respond = DBHelper.getSyncRespond(identificationInfo);
                ByteBuffer buffer = XMLUtil.marshal(respond);
                channel.write(buffer);
//                System.out.println(new String(buffer.array()));
                if (respond.getSucceed()) {
                    ConnectionSteps.Messaging messageInfo = ConnectionSteps.Messaging.MESSAGE_INFO;
                    messageInfo.setAttachment(identificationInfo);
                    key.attach(messageInfo);
                } else
                    key.cancel();
                break;
            }
            case IDLE: {
                break;
            }
            case MESSAGE_INFO: {
                ConnectionSteps.Messaging attachment = (ConnectionSteps.Messaging) key.attachment();
                IdentificationInfo identificationInfo = (IdentificationInfo) attachment.getAttachment();
                DBHelper.getMessages(identificationInfo);
//                MessageInfo messageInfo;
//                ByteBuffer buffer = XMLUtil.marshal(messageInfo);
//                buffer = ChannelHelper.encrypt(buffer, symmetricKey);
//                channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                key.interestOps(SelectionKey.OP_READ);
                key.attach(ConnectionSteps.Messaging.MESSAGE_RESPOND);
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
                key.interestOps(SelectionKey.OP_WRITE);
                ConnectionSteps.Messaging respond = ConnectionSteps.Messaging.SYNC_RESPOND;
                respond.setAttachment(identificationInfo);
                key.attach(respond);
                break;
            }
            case IDLE: {
                break;
            }
            case MESSAGE_RESPOND: {
//                data = ChannelHelper.decrypt(data, symmetricKey);
                RespondInfo respondInfo = XMLUtil.unmarshal(RespondInfo.class, data);
                System.out.println(respondInfo);
                key.attach(ConnectionSteps.Messaging.MESSAGE_INFO);
                break;
            }
        }
    }
}
