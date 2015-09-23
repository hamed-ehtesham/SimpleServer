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
    private RegistrationRespondInfo respond;

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

    public RegistrationRespondInfo getRespond() {
        return respond;
    }

    public void setRespond(RegistrationRespondInfo respond) {
        this.respond = respond;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        RegisterRequest request = new RegisterRequest("ali_j@gmail.com", "i,d[", "ali", "jafari", "aliJ");
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
                    RegistrationRespondInfo respondInfo = XMLUtil.unmarshal(RegistrationRespondInfo.class, data);
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

            switch ((ConnectionSteps.Registration) key.attachment()) {
                case SYMMETRIC_KEY: {
                    if (sealedObject != null) {
                        ChannelHelper.writeObject(channel, sealedObject);
//                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                        ObjectOutputStream oos = new ObjectOutputStream(bos);
//                        SealedObject[] soa = new SealedObject[]{sealedObject};
//
//                        oos.writeObject(soa);
//                        oos.flush();
//
//                        ByteBuffer buffer = ByteBuffer.wrap(bos.toByteArray());
//                        channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
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
                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                    buffer = aesEncryptionUtil.encrypt(buffer);
                    channel.write(buffer);
//                    System.out.println(new String(buffer.array()));
                    key.interestOps(SelectionKey.OP_READ);
                    ConnectionSteps.Registration respond = ConnectionSteps.Registration.REG_RESPOND;
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
