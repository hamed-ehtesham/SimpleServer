import javax.crypto.SealedObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

    /**
     * @param args
     */
    public static void main(String[] args) {
        LoginRequest request = new LoginRequest("alijaf@gmail.com", "i,d[");
        request.request();
    }

    class request extends ClientHandler {
        private SealedObject sealedObject;

        public request(String ADDRESS, int PORT) {
            super(ADDRESS, PORT);
        }

        @Override
        protected void read(SelectionKey key) throws IOException {
            ByteArrayOutputStream bos = ChannelHelper.read(key);
            byte[] data = bos.toByteArray();
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
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        SealedObject[] soa = new SealedObject[]{sealedObject};
                        oos.writeObject(soa);
                        oos.flush();

                        ByteBuffer buffer = ByteBuffer.wrap(bos.toByteArray());
                        channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                        key.attach(ConnectionSteps.Login.LOGIN_INFO);
                    }

                    break;
                }
                case LOGIN_INFO: {
                    LoginRequestInfo requestInfo = new LoginRequestInfo();
                    requestInfo.setEmail(getEmail());
                    requestInfo.setPassword(getPassword());

                    ByteBuffer buffer = XMLUtil.marshal(requestInfo);
                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                    buffer = aesEncryptionUtil.encrypt(buffer);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    ConnectionSteps.Login respond = ConnectionSteps.Login.LOGIN_RESPOND;
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
        Thread thread = new Thread(new request("localhost", 8511));
        thread.start();
    }
}
