import javax.crypto.SealedObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/19/2015.
 */
public class RegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String nickname;
    private RespondInfo respond;

    public RegisterRequest(String email, String password, String firstName, String lastName, String nickname) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickname = nickname;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public RespondInfo getRespond() {
        return respond;
    }

    public void setRespond(RespondInfo respond) {
        this.respond = respond;
    }

    public static void main(String[] args) {
        RegisterRequest request = new RegisterRequest("javad.mohamma@gmail.com", "i,d[", "ali", "jafari", "aliJ");
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
            readKey.attach(ConnectionSteps.Registration.PUBLIC_KEY);
        }

        @Override
        protected void read(SelectionKey key) throws IOException {
            byte[] data = ChannelHelper.read(key);
            switch ((ConnectionSteps.Registration) key.attachment()) {
                case PUBLIC_KEY: {
                    KeyInfo keyInfo = XMLUtil.unmarshal(KeyInfo.class, data);
                    System.out.println(keyInfo);
                    encryptSymmetricKey(keyInfo);
                    key.interestOps(SelectionKey.OP_WRITE);
                    key.attach(ConnectionSteps.Registration.SYMMETRIC_KEY);
                    break;
                }
                case REG_RESPOND: {
                    RespondInfo respondInfo = XMLUtil.unmarshal(RespondInfo.class, data);
                    setRespond(respondInfo);
                    System.out.println(respondInfo);
                    if(respondInfo.getSucceed())
                    {
                        AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                        System.out.println(getPassword());
//                        requestInfo.setPassword(aesEncryptionUtil.decrypt(requestInfo.getPassword()));
                        LoginRequest request = new LoginRequest(getEmail(), getPassword());
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

            switch ((ConnectionSteps.Registration) key.attachment()) {
                case SYMMETRIC_KEY: {
                    if (sealedObject != null) {
                        ChannelHelper.writeObject(channel, sealedObject);
                        key.attach(ConnectionSteps.Registration.REG_INFO);
                    }
                    break;
                }
                case REG_INFO: {
                    RegistrationRequestInfo requestInfo = new RegistrationRequestInfo();
                    requestInfo.setEmail(getEmail());
                    requestInfo.setPassword(getPassword());
                    requestInfo.setFirstName(getFirstName());
                    requestInfo.setLastName(getLastName());
                    requestInfo.setNickname(getNickname());

                    ByteBuffer buffer = XMLUtil.marshal(requestInfo);
                    buffer = ChannelHelper.encrypt(buffer, symmetricKey);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    key.attach(ConnectionSteps.Registration.REG_RESPOND);

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
