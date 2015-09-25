import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/23/2015.
 */
public class MessageSyncRequest {
    String sessionID;
    private IdentificationInfo identificationInfo;
    String salt;
    private final int latency;

    public MessageSyncRequest(IdentificationInfo identificationInfo, String salt, int latency) {
        this.sessionID = identificationInfo.getSessionID();
        this.identificationInfo = identificationInfo;
        this.salt = salt;
        this.latency = latency;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public static void main(String[] args) {
        IdentificationInfo identificationInfo = new IdentificationInfo();
        identificationInfo.setEmail("ali_jafari2@gmail.com");
        identificationInfo.setSessionID("L9pLGkVjwwGSBN6JyxrsDPWZ+J8AGqC2QgntsFpsVhOBvRvLV3abZvs5FWgCfvCPnDQcGNx4FzBSIr8NmLyvzw==");
        String salt = "817psc5k724k7u20eejng0p6ai";
        MessageSyncRequest request = new MessageSyncRequest(identificationInfo, salt, 1000);
        request.request();
    }

    class Request extends ClientHandler {

        public Request(String ADDRESS, int PORT) {
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
            readKey.attach(ConnectionSteps.Messaging.IDENTIFICATION);
        }

        @Override
        protected void write(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();

            switch ((ConnectionSteps.Messaging) key.attachment()) {
                case IDENTIFICATION: {
                    ByteBuffer buffer = XMLUtil.marshal(identificationInfo);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Messaging.SYNC_RESPOND);
                    break;
                }
                case MESSAGE_RESPOND: {
                    RespondInfo respondInfo = new RespondInfo();
                    respondInfo.setSucceed(true);
                    ByteBuffer buffer = XMLUtil.marshal(respondInfo);
                    buffer = ChannelHelper.encrypt(buffer, symmetricKey);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Messaging.SYNC_RESPOND);
//                    key.cancel();
//                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        @Override
        protected void read(SelectionKey key) throws IOException {
            byte[] data = ChannelHelper.read(key);
            switch ((ConnectionSteps.Messaging) key.attachment()) {
                case SYNC_RESPOND: {
                    RespondInfo respondInfo = XMLUtil.unmarshal(RespondInfo.class, data);
                    if (respondInfo.getSucceed()) {
                        System.out.println(respondInfo);
                        key.interestOps(SelectionKey.OP_READ);
                        ConnectionSteps.Messaging respond = ConnectionSteps.Messaging.MESSAGE_INFO;
//                    respond.setAttachment(messageInfo);
                        key.attach(respond);
                    } else {
                        key.cancel();
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
                case MESSAGE_INFO: {
                    data = ChannelHelper.decrypt(data, symmetricKey);
                    MessageInfo messageInfo = XMLUtil.unmarshal(MessageInfo.class, data);
                    System.out.println(messageInfo);
                    key.interestOps(SelectionKey.OP_WRITE);
                    ConnectionSteps.Messaging respond = ConnectionSteps.Messaging.MESSAGE_RESPOND;
                    respond.setAttachment(messageInfo);
                    key.attach(respond);
                    break;
                }
            }
        }
    }

    public void request() {
        Thread thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                Request request = new Request("localhost", 8515);
                request.setGenerateSymmetricKey(false);
                request.setSymmetricKey(getSalt());
                request.setPrintStatus(false);
                Thread thread1 = new Thread(request);
                thread1.start();
                try {
                    Thread.sleep(latency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
