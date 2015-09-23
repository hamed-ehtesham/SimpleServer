import javax.crypto.SealedObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Created by Hamed on 9/19/2015.
 */
public class RegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String nickname;
    private String symmetricKey;
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
        RegisterRequest request = new RegisterRequest("alij@gmail.com","i,d[","ali","jafari","aliJ");
        request.request();
    }

    class request implements Runnable {

        private Selector selector;
        private SealedObject sealedObject;

        @Override
        public void run() {
            SocketChannel channel;
            try {
                selector = Selector.open();
                channel = SocketChannel.open();
                channel.configureBlocking(false);

                symmetricKey = SymmetricUtil.nextSymmetricKey();

                channel.register(selector, SelectionKey.OP_CONNECT);
                channel.connect(new InetSocketAddress("127.0.0.1", 8511));

                while (!Thread.interrupted()) {

                    selector.select(1000);

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (!key.isValid()) continue;

                        if (key.isConnectable()) {
                            System.out.println("I am connected to the server");
                            connect(key);
                        }
                        if (key.isWritable()) {
                            write(key);
                        }
                        if (key.isReadable()) {
                            read(key);
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                close();
            }
        }

        private void close() {
            try {
                selector.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void read(SelectionKey key) throws IOException {
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

        private void write(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();

            switch ((ConnectionSteps.Registration) key.attachment()) {
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

        private void connect(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            channel.configureBlocking(false);

            //Prepare key for receive server's public key from server
            SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);
            readKey.attach(ConnectionSteps.Registration.PUBLIC_KEY);
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
        //connect to server

        //get server's public key

        //generate symmetric key and send it to server using server's public key

        //send username and password to server using symmetric key

        //get response from server and act accordingly


        Thread thread = new Thread(new request());
        thread.start();
    }
}
