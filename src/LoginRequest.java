import javax.crypto.SealedObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Mohammad Amin on 23/09/2015.
 */
public class LoginRequest {
    private String email;
    private String password;
    private LoginRespondInfo respond;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LoginRespondInfo getRespond() {
        return respond;
    }

    public void setRespond(LoginRespondInfo respond) {
        this.respond = respond;
    }

    public static void main(String[] args) {
        LoginRequest request = new LoginRequest("ali_jaf@gmail.com", "i,d[");
        request.request();
    }

    class request extends ClientHandler {
        private SealedObject sealedObject;

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

            //Prepare key for receive server's public key from server
            SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);
            readKey.attach(ConnectionSteps.Login.PUBLIC_KEY);
        }

        @Override
        protected void read(SelectionKey key) throws IOException {
            byte[] data = ChannelHelper.read(key);
            switch ((ConnectionSteps.Login) key.attachment()) {
                case PUBLIC_KEY: {
                    KeyInfo keyInfo = XMLUtil.unmarshal(KeyInfo.class, data);
                    System.out.println(keyInfo);
                    encryptSymmetricKey(keyInfo);
                    key.interestOps(SelectionKey.OP_WRITE);
                    key.attach(ConnectionSteps.Login.SYMMETRIC_KEY);
                    break;
                }
                case LOGIN_RESPOND: {
                    LoginRespondInfo respondInfo = XMLUtil.unmarshal(LoginRespondInfo.class, data);
                    setRespond(respondInfo);
                    System.out.println(respondInfo);
                    if(respondInfo.getSucceed()) {
                        IdentificationInfo identificationInfo = new IdentificationInfo();
                        identificationInfo.setSessionID(respondInfo.getSessionID());
                        identificationInfo.setEmail(getEmail());
                        MessageSyncRequest request = new MessageSyncRequest(identificationInfo,symmetricKey, 1000);
                        request.request();
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

            switch ((ConnectionSteps.Login) key.attachment()) {
                case SYMMETRIC_KEY: {
                    if (sealedObject != null) {
                        ChannelHelper.writeObject(channel, sealedObject);
                        key.attach(ConnectionSteps.Login.LOGIN_INFO);
                    }

                    break;
                }
                case LOGIN_INFO: {
                    LoginRequestInfo requestInfo = new LoginRequestInfo();
                    requestInfo.setEmail(getEmail());
                    requestInfo.setPassword(getPassword());

                    ByteBuffer buffer = XMLUtil.marshal(requestInfo);
                    buffer = ChannelHelper.encrypt(buffer,symmetricKey);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Login.LOGIN_RESPOND);

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
        Thread thread = new Thread(new request("localhost", 8513));
        thread.start();
    }
}
