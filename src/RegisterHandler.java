import javax.crypto.SealedObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Created by Hamed on 9/19/2015.
 */
public class RegisterHandler implements Runnable {
    public final static String ADDRESS = "127.0.0.1";
    public final static int PORT = 8511;
    public final static long TIMEOUT = 10000;

    private RSAEncryptionUtil rsaEncryptionUtil;

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private String symmetricKey;

    public static void main(String[] args) {
        RegisterHandler registerHandler = new RegisterHandler();
        Thread thread = new Thread(registerHandler);
        thread.start();
    }

    /**
     * This hashmap is important. It keeps track of the data that will be written to the clients.
     * This is needed because we read/write asynchronously and we might be reading while the server
     * wants to write. In other words, we tell the Selector we are ready to write (SelectionKey.OP_WRITE)
     * and when we get a key for writting, we then write from the Hashmap. The write() method explains this further.
     */

    public RegisterHandler() {
        init();
    }

    private void init() {
        System.out.println("initializing server");
        // We do not want to call init() twice and recreate the selector or the serverChannel.
        if (selector != null) return;
        if (serverChannel != null) return;

        try {
            // This is how you open a Selector
            selector = Selector.open();
            // This is how you open a ServerSocketChannel
            serverChannel = ServerSocketChannel.open();
            // You MUST configure as non-blocking or else you cannot register the serverChannel to the Selector.
            serverChannel.configureBlocking(false);
            // bind to the address that you will use to Serve.
            serverChannel.socket().bind(new InetSocketAddress(ADDRESS, PORT));
            // Generate Asymmetric keys and store them
            try {
                rsaEncryptionUtil = new RSAEncryptionUtil();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /**
             * Here you are registering the serverSocketChannel to accept connection, thus the OP_ACCEPT.
             * This means that you just told your selector that this channel will be used to accept connections.
             * We can change this operation later to read/write, more on this later.
             */
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Now accepting connections...");
        try {
            // A run the server as long as the thread is not interrupted.
            while (!Thread.currentThread().isInterrupted()) {
                /**
                 * selector.select(TIMEOUT) is waiting for an OPERATION to be ready and is a blocking call.
                 * For example, if a client connects right this second, then it will break from the select()
                 * call and run the code below it. The TIMEOUT is not needed, but its just so it doesn't
                 * block undefinitely.
                 */
                selector.select(TIMEOUT);

                /**
                 * If we are here, it is because an operation happened (or the TIMEOUT expired).
                 * We need to get the SelectionKeys from the selector to see what operations are available.
                 * We use an iterator for this.
                 */
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    // remove the key so that we don't process this OPERATION again.
                    keys.remove();

                    // key could be invalid if for example, the client closed the connection.
                    if (!key.isValid()) {
                        continue;
                    }
                    /**
                     * In the server, we start by listening to the OP_ACCEPT when we register with the Selector.
                     * If the key from the keyset is Acceptable, then we must get ready to accept the client
                     * connection and do something with it. Go read the comments in the accept method.
                     */
                    if (key.isAcceptable()) {
                        System.out.println("Accepting connection");
                        accept(key);
                    }
                    /**
                     * If you already read the comments in the accept() method, then you know we changed
                     * the OPERATION to OP_WRITE. This means that one of these keys in the iterator will return
                     * a channel that is writable (key.isWritable()). The write() method will explain further.
                     */
                    else if (key.isWritable()) {
                        System.out.println("Writing...");
                        write(key);
                    }
                    /**
                     * If you already read the comments in the write method then you understand that we registered
                     * the OPERATION OP_READ. That means that on the next Selector.select(), there is probably a key
                     * that is ready to read (key.isReadable()). The read() method will explain further.
                     */
                    else if (key.isReadable()) {
                        System.out.println("Reading connection");
                        read(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

    }

    /**
     * We registered this channel in the Selector. This means that the SocketChannel we are receiving
     * back from the key.channel() is the same channel that was used to register the selector in the accept()
     * method. Again, I am just explaning as if things are synchronous to make things easy to understand.
     * This means that later, we might register to write from the read() method (for example).
     */
    private void write(SelectionKey key) throws IOException {
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
                            String sessionID = SymmetricUtil.getSessionID(requestInfo.getEmail(), channel.getRemoteAddress(), symmetricKey);
                            respondInfo.setSessionID(sessionID);
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

    // Nothing special, just closing our selector and socket.
    private void closeConnection() {
        System.out.println("Closing server down");
        if (selector != null) {
            try {
                selector.close();
                serverChannel.socket().close();
                serverChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //Prepare key for send server's public key to client
        SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
        writeKey.attach(ConnectionSteps.Registration.PUBLIC_KEY);
    }

    /**
     * We read data from the channel. In this case, my server works as an echo, so it calls the echo() method.
     * The echo() method, sets the server in the WRITE OPERATION. When the while loop in run() happens again,
     * one of those keys from Selector.select() will be key.isWritable() and this is where the actual
     * write will happen by calling the write() method.
     */
    private void read(SelectionKey key) throws IOException {
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
