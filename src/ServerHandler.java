import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/**
 * Created by Hamed on 9/23/2015.
 */
public abstract class ServerHandler implements Runnable {
    public final String ADDRESS;
    public final int PORT;
    public final long TIMEOUT;

    protected RSAEncryptionUtil rsaEncryptionUtil;

    protected ServerSocketChannel serverChannel;
    protected Selector selector;

    public ServerHandler(String ADDRESS, int PORT, long TIMEOUT) {
        this.ADDRESS = ADDRESS;
        this.PORT = PORT;
        this.TIMEOUT = TIMEOUT;
    }

    protected void init() {
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

            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (!(serverChannel.socket().isBound() && !serverChannel.socket().isClosed()))
            Thread.currentThread().interrupt();
        else
            System.out.println("Now accepting connections...");
        try {
            // A run the server as long as the thread is not interrupted.
            while (!Thread.currentThread().isInterrupted()) {

                selector.select(TIMEOUT);


                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    // remove the key so that we don't process this OPERATION again.
                    keys.remove();

                    // key could be invalid if for example, the client closed the connection.
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        System.out.println("Accepting connection");
                        accept(key);
                    } else if (key.isWritable()) {
                        System.out.println("Writing...");
                        write(key);
                    } else if (key.isReadable()) {
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

    protected abstract void accept(SelectionKey key) throws IOException;

    protected abstract void write(SelectionKey key) throws IOException;

    protected abstract void read(SelectionKey key) throws IOException;

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
}
