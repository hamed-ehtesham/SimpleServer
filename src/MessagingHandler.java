import javax.crypto.SealedObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/23/2015.
 */
public class MessagingHandler extends ServerHandler {
    private String symmetricKey;

    public MessagingHandler(String ADDRESS, int PORT, long TIMEOUT) {
        super(ADDRESS, PORT, TIMEOUT);
        init();
    }

    public static void main(String[] args) {
        MessagingHandler messagingHandler = new MessagingHandler("localhost",8511,10000);
        Thread thread = new Thread(messagingHandler);
        thread.start();
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
        writeKey.attach(ConnectionSteps.Registration.PUBLIC_KEY);
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

                    if(requestInfo != null) {
                        respondInfo = RegistrationDBHelper.register(requestInfo);
                        if(respondInfo.getSucceed()) {
                            // TODO: automatic login to server using email and password
//                            String sessionID = SymmetricUtil.getSessionID(requestInfo.getEmail(), channel.getRemoteAddress(), symmetricKey);
//                            respondInfo.setSessionID(sessionID);
                        }
                    } else {
                        throw new IOException("requestInfo not received!");
                    }
                } catch (Exception e) {
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
        switch ((ConnectionSteps.Registration) key.attachment()) {
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
