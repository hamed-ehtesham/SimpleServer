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
 * Created by Mohammad Amin on 23/09/2015.
 */
public class LoginHandler extends ServerHandler {

    public LoginHandler(String ADDRESS, int PORT, long TIMEOUT) {
        super(ADDRESS, PORT, TIMEOUT);
        init();
    }

    public static void main(String[] args) {
        LoginHandler loginHandler = new LoginHandler("localhost",8513,10000);
        Thread thread = new Thread(loginHandler);
        thread.start();
    }

    @Override
    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Prepare key for send server's public key to client
        SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
        writeKey.attach(ConnectionSteps.Login.PUBLIC_KEY);
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        switch ((ConnectionSteps.Login) key.attachment()) {
            case PUBLIC_KEY: {
                ChannelHelper.writePublicKey(channel, rsaEncryptionUtil);
                ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Login.SYMMETRIC_KEY);

                break;
            }
            case LOGIN_RESPOND: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                String symmetricKey = (String) attachment.get(0);
                LoginRequestInfo requestInfo = (LoginRequestInfo) attachment.get(1);
                LoginRespondInfo respondInfo = new LoginRespondInfo();
                try {

                    if(requestInfo != null) {
                        respondInfo = DBHelper.login(requestInfo, symmetricKey, channel.getRemoteAddress());
                        if(respondInfo.getSucceed()) {
                            //Login successful
                        }
                    } else {
                        throw new IOException("requestInfo not received!");
                    }
                } catch (Exception e) {
                    respondInfo.setSucceed(false);
                    respondInfo.setMessage("Login failure");
                }


                ByteBuffer buffer = XMLUtil.marshal(respondInfo);
                channel.write(buffer);
                key.cancel();

                break;
            }
        }

    }

    @Override
    protected void read(SelectionKey key) throws IOException {
        byte[] data = ChannelHelper.read(key);
        switch ((ConnectionSteps.Login) key.attachment()) {
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
                    ConnectionSteps.attach(key, SelectionKey.OP_READ, ConnectionSteps.Login.LOGIN_INFO,symmetricKey);
                }
                break;
            }
            case LOGIN_INFO: {
                ArrayList<Object> attachment = ConnectionSteps.attachment(key);
                String symmetricKey = (String) attachment.get(0);
                data = ChannelHelper.decrypt(data, symmetricKey);
                LoginRequestInfo requestInfo = XMLUtil.unmarshal(LoginRequestInfo.class, data);
                System.out.println(requestInfo);
                ConnectionSteps.attach(key, SelectionKey.OP_WRITE, ConnectionSteps.Login.LOGIN_RESPOND, symmetricKey, requestInfo);
                break;
            }
        }
    }
}
