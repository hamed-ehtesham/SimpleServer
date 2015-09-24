import javax.crypto.SealedObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Mohammad Amin on 23/09/2015.
 */
public class LoginHandler extends ServerHandler {
    private String symmetricKey;

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
                KeyInfo keyInfo = new KeyInfo();
                keyInfo.setKey(rsaEncryptionUtil.getPublicKey());

                ByteBuffer buffer = XMLUtil.marshal(keyInfo);
                channel.write(buffer);
//                System.out.println(new String(buffer.array()));
                key.interestOps(SelectionKey.OP_READ);
                key.attach(ConnectionSteps.Login.SYMMETRIC_KEY);

                break;
            }
            case LOGIN_RESPOND: {
                LoginRespondInfo respondInfo = new LoginRespondInfo();
                ConnectionSteps.Login respond = (ConnectionSteps.Login) key.attachment();
                try {
                    LoginRequestInfo requestInfo = (LoginRequestInfo) respond.getAttachment();

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
//                System.out.println(new String(buffer.array()));
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
                SealedObject sealedObject = ChannelHelper.readObject(data, SealedObject.class);
                if (sealedObject != null) {
                    try {
                        symmetricKey = RSAEncryptionUtil.decryptByPrivateKey(sealedObject, rsaEncryptionUtil.getPrivateKey());
                        System.out.println("Symmetric key received: " + symmetricKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    key.attach(ConnectionSteps.Login.LOGIN_INFO);
                }
                break;
            }
            case LOGIN_INFO: {
                data = ChannelHelper.decrypt(data,symmetricKey);
                LoginRequestInfo requestInfo = XMLUtil.unmarshal(LoginRequestInfo.class, data);
                System.out.println(requestInfo);
                key.interestOps(SelectionKey.OP_WRITE);
                ConnectionSteps.Login respond = ConnectionSteps.Login.LOGIN_RESPOND;
                respond.setAttachment(requestInfo);
                key.attach(respond);
                break;
            }
        }
    }
}
