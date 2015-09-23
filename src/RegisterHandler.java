import javax.crypto.SealedObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/19/2015.
 */
public class RegisterHandler extends ServerHandler {
    private String symmetricKey;

    public RegisterHandler(String ADDRESS, int PORT, long TIMEOUT) {
        super(ADDRESS, PORT, TIMEOUT);
        init();
    }

    public static void main(String[] args) {
        RegisterHandler registerHandler = new RegisterHandler("localhost", 8511, 10000);
        Thread thread = new Thread(registerHandler);
        thread.start();
    }

    @Override
    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Prepare key for send server's public key to client
        SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
        writeKey.attach(ConnectionSteps.Registration.PUBLIC_KEY);
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        switch ((ConnectionSteps.Registration) key.attachment()) {
            case PUBLIC_KEY: {
                KeyInfo keyInfo = new KeyInfo();
                keyInfo.setKey(rsaEncryptionUtil.getPublicKey());

                ByteBuffer buffer = XMLUtil.marshal(keyInfo);
                channel.write(buffer);
//                System.out.println(new String(buffer.array()));
                key.interestOps(SelectionKey.OP_READ);
                key.attach(ConnectionSteps.Registration.SYMMETRIC_KEY);

                break;
            }
            case REG_RESPOND: {
                RegistrationRespondInfo respondInfo = new RegistrationRespondInfo();
                ConnectionSteps.Registration respond = (ConnectionSteps.Registration) key.attachment();
                try {
                    RegistrationRequestInfo requestInfo = (RegistrationRequestInfo) respond.getAttachment();

                    if (requestInfo != null) {
                        AESEncryptionUtil encryptionUtil = new AESEncryptionUtil(symmetricKey);
                        requestInfo.setPassword(encryptionUtil.encrypt(requestInfo.getPassword()));
                        respondInfo = DBHelper.register(requestInfo, symmetricKey);
                    } else {
                        throw new IOException("requestInfo not received!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    respondInfo.setSucceed(false);
                    respondInfo.setMessage("Register failure");
                }


                ByteBuffer buffer = XMLUtil.marshal(respondInfo);
                channel.write(buffer);
//                System.out.println(new String(buffer.array()));
                key.cancel();

                break;
            }
        }

    }

    @Override
    protected void read(SelectionKey key) throws IOException {
        ByteArrayOutputStream bos = ChannelHelper.read(key);
        byte[] data = bos.toByteArray();
        switch ((ConnectionSteps.Registration) key.attachment()) {
            case SYMMETRIC_KEY: {
                SealedObject sealedObject = ChannelHelper.readObject(data, SealedObject.class);
                if (sealedObject != null) {
                    try {
                        symmetricKey = RSAEncryptionUtil.decryptByPrivateKey(sealedObject, rsaEncryptionUtil.getPrivateKey());
                        System.out.println("Symmetric key received: " + symmetricKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    key.attach(ConnectionSteps.Registration.REG_INFO);
                }
                break;
            }
            case REG_INFO: {
                AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                data = aesEncryptionUtil.decrypt(data);
                RegistrationRequestInfo requestInfo = XMLUtil.unmarshal(RegistrationRequestInfo.class, data);
                System.out.println(requestInfo);
                key.interestOps(SelectionKey.OP_WRITE);
                ConnectionSteps.Registration respond = ConnectionSteps.Registration.REG_RESPOND;
                respond.setAttachment(requestInfo);
                key.attach(respond);
                break;
            }
        }
    }
}
