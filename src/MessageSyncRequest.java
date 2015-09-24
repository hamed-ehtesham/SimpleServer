import javax.crypto.SealedObject;
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
    Request request;
    MessageInfo messageInfo;

    public MessageSyncRequest(String sessionID) {
        this.sessionID = sessionID;
        //get salt from db
        setSalt(DBHelper.getSessionInfo(sessionID));
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

    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo) {
        this.messageInfo = messageInfo;
    }

    public static void main(String[] args) {
        MessageSyncRequest request = new MessageSyncRequest("L9pLGkVjwwGSBN6JyxrsDPWZ+J8AGqC2QgntsFpsVhOBvRvLV3abZvs5FWgCfvCPnDQcGNx4FzBSIr8NmLyvzw==");
        request.request();
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContent("Hello Server!");
        MessageInfo.Recipient recipient = new MessageInfo.Recipient();
        recipient.setrType(MessageInfo.Recipient.RecipientType.PERSON);
        messageInfo.setRecipient(recipient);
        messageInfo.setSender("Hamed");
        request.writeMessage(messageInfo);
    }

    class Request extends ClientHandler {
        private SealedObject sealedObject;

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

            //Prepare key for receive server's public key from server
            SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);
            readKey.attach(ConnectionSteps.Messaging.IDLE);
        }

        @Override
        protected void read(SelectionKey key) throws IOException {
            byte[] data = ChannelHelper.read(key);
            switch ((ConnectionSteps.Messaging) key.attachment()) {
                case IDLE: {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case MESSAGE_INFO: {
                    data = ChannelHelper.decrypt(data, symmetricKey);
                    MessageInfo messageInfo = XMLUtil.unmarshal(MessageInfo.class, data);
                    System.out.println(messageInfo);
                    key.interestOps(SelectionKey.OP_WRITE);
                    ConnectionSteps.Messaging respond = ConnectionSteps.Messaging.MESSAGE_RESPOND;
//                    respond.setAttachment(messageInfo);
                    key.attach(respond);
                    break;
                }
                case MESSAGE_RESPOND: {
                    data = ChannelHelper.decrypt(data, symmetricKey);
                    RespondInfo respondInfo = XMLUtil.unmarshal(RespondInfo.class, data);
                    System.out.println(respondInfo);
                    key.attach(ConnectionSteps.Messaging.MESSAGE_INFO);
                    break;
                }
            }
        }

        @Override
        protected void write(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();

            switch ((ConnectionSteps.Messaging) key.attachment()) {
                case IDENTIFICATION: {
                    ByteBuffer buffer = XMLUtil.marshal(identificationInfo);
                    buffer = ChannelHelper.encrypt(buffer,symmetricKey);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Messaging.MESSAGE_RESPOND);
                    break;
                }
                case IDLE: {
                    break;
                }
                case MESSAGE_INFO: {
                    messageInfo.setSession(getSessionID());
                    ByteBuffer buffer = XMLUtil.marshal(messageInfo);
                    buffer = ChannelHelper.encrypt(buffer,symmetricKey);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Messaging.MESSAGE_RESPOND);
                    break;
                }
                case MESSAGE_RESPOND: {
                    RespondInfo respondInfo = new RespondInfo();
                    respondInfo.setSucceed(true);
                    ByteBuffer buffer = XMLUtil.marshal(respondInfo);
                    buffer = ChannelHelper.encrypt(buffer,symmetricKey);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Messaging.MESSAGE_INFO);
                    break;
                }
            }
        }

        private void encryptSymmetricKey(KeyInfo keyInfo) {
            try {
                sealedObject = RSAEncryptionUtil.encryptByPublicKey(symmetricKey, keyInfo.key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void request() {
        request = new Request("localhost", 8515);
        request.symmetricKey = getSalt();
        Thread thread = new Thread(request);
        thread.start();
    }

    public void writeMessage(MessageInfo messageInfo) {
        setMessageInfo(messageInfo);
        SelectionKey selectionKey = request.getSelectionKey();
        System.out.println("im here");
        selectionKey.attach(ConnectionSteps.Messaging.MESSAGE_INFO);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }
}
