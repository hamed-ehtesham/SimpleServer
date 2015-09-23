import javax.crypto.SealedObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketAddress;
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

    /**
     * Since we are accepting, we must instantiate a serverSocketChannel by calling key.channel().
     * We use this in order to get a socketChannel (which is like a socket in I/O) by calling
     * serverSocketChannel.accept() and we register that channel to the selector to listen
     * to a WRITE OPERATION. I do this because my server sends a hello message to each
     * client that connects to it. This doesn't mean that I will write right NOW. It means that I
     * told the selector that I am ready to write and that next time Selector.select() gets called
     * it should give me a key with isWritable(). More on this in the write() method.
     */
    @Override
    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Prepare key for send server's public key to client
        SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
        writeKey.attach(ConnectionSteps.Login.PUBLIC_KEY);
    }

    /**
     * We registered this channel in the Selector. This means that the SocketChannel we are receiving
     * back from the key.channel() is the same channel that was used to register the selector in the accept()
     * method. Again, I am just explaning as if things are synchronous to make things easy to understand.
     * This means that later, we might register to write from the read() method (for example).
     */
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
                        respondInfo = RegistrationDBHelper.login(requestInfo, symmetricKey, channel.socket().getInetAddress().toString());
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

    /**
     * We read data from the channel. In this case, my server works as an echo, so it calls the echo() method.
     * The echo() method, sets the server in the WRITE OPERATION. When the while loop in run() happens again,
     * one of those keys from Selector.select() will be key.isWritable() and this is where the actual
     * write will happen by calling the write() method.
     */
    @Override
    protected void read(SelectionKey key) throws IOException {
        ByteArrayOutputStream bos = ChannelHelper.read(key);
        byte[] data = bos.toByteArray();
        switch ((ConnectionSteps.Login) key.attachment()) {
            case SYMMETRIC_KEY: {
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis);
                SealedObject sealedObject = null;
                try {
                    SealedObject[] sealedObjects = (SealedObject[]) ois.readObject();
                    sealedObject = sealedObjects[0];
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
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
                AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
                data = aesEncryptionUtil.decrypt(data);
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
