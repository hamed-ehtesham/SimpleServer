import javax.crypto.SealedObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Hamed on 9/19/2015.
 */
public class RegisterHandler extends ServerHandler {

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
                ChannelHelper.writePublicKey(channel, rsaEncryptionUtil);
                ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Registration.SYMMETRIC_KEY);

                break;
            }
            case REG_RESPOND: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                String symmetricKey = (String) attachment.get(0);
                RegistrationRequestInfo requestInfo = (RegistrationRequestInfo) attachment.get(1);
                RespondInfo respondInfo = new RespondInfo();
                try {
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
        byte[] data = ChannelHelper.read(key);
        switch ((ConnectionSteps.Registration) key.attachment()) {
            case SYMMETRIC_KEY: {
                String symmetricKey = null;
                SealedObject sealedObject = ChannelHelper.readObject(data, SealedObject.class);
                if (sealedObject != null) {
                    try {
                        symmetricKey = RSAEncryptionUtil.decryptByPrivateKey(sealedObject, rsaEncryptionUtil.getPrivateKey());
                        System.out.println("Symmetric key received: " + symmetricKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Registration.REG_INFO, symmetricKey);
                }
                break;
            }
            case REG_INFO: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                String symmetricKey = (String) attachment.get(0);
                data = ChannelHelper.decrypt(data, symmetricKey);
                RegistrationRequestInfo requestInfo = XMLUtil.unmarshal(RegistrationRequestInfo.class, data);
                System.out.println(requestInfo);
                ConnectionSteps.attach(key, SelectionKey.OP_WRITE, ConnectionSteps.Registration.REG_RESPOND, symmetricKey, requestInfo);
                break;
            }
        }
    }
}
