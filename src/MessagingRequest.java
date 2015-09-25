import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/23/2015.
 */
public class MessagingRequest {
    String sessionID;
    private IdentificationInfo identificationInfo;
    String salt;
    private final int latency;

    public MessagingRequest(IdentificationInfo identificationInfo, String salt, int latency) {
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
        MessagingRequest request = new MessagingRequest(identificationInfo, salt, 1000);
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContent("salam!");
        messageInfo.setSender(identificationInfo.getEmail());
        MessageInfo.Recipient recipient = new MessageInfo.Recipient();
        recipient.setrType(MessageInfo.Recipient.RecipientType.PERSON);
        recipient.setRecipient("ali_jaf@gmail.com");
        messageInfo.setRecipient(recipient);
        messageInfo.setSession(request.getSessionID());
        request.request(messageInfo);
    }

    class Request extends ClientHandler {
        private MessageInfo messageInfo;

        public Request(String ADDRESS, int PORT) {
            super(ADDRESS, PORT);
        }

        public MessageInfo getMessageInfo() {
            return messageInfo;
        }

        public void setMessageInfo(MessageInfo messageInfo) {
            this.messageInfo = messageInfo;
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
                    ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Messaging.SYNC_RESPOND);
                    break;
                }
                case MESSAGE_INFO: {
                    ByteBuffer buffer = XMLUtil.marshal(getMessageInfo());
                    buffer = ChannelHelper.encrypt(buffer, getSymmetricKey());
                    channel.write(buffer);
                    ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Messaging.MESSAGE_RESPOND);
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
                        ConnectionSteps.attach(key, SelectionKey.OP_WRITE, ConnectionSteps.Messaging.MESSAGE_INFO);
                    } else {
                        key.cancel();
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
                case MESSAGE_RESPOND: {
                    data = ChannelHelper.decrypt(data, symmetricKey);
                    RespondInfo respondInfo = XMLUtil.unmarshal(RespondInfo.class, data);
                    System.out.println(respondInfo);
                    key.cancel();
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void request(MessageInfo messageInfo) {
        Request request = new Request("localhost", 8516);
        request.setGenerateSymmetricKey(false);
        request.setSymmetricKey(getSalt());
        request.setMessageInfo(messageInfo);
        Thread thread1 = new Thread(request);
        thread1.start();
    }
}
