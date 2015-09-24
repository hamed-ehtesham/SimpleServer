import javax.crypto.SealedObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by Hamed on 9/23/2015.
 */
public abstract class ClientHandler implements Runnable {
    public final String ADDRESS;
    public final int PORT;
    public final long TIMEOUT;

    protected SelectionKey selectionKey;
    protected Selector selector;
    protected String symmetricKey;

    public ClientHandler(String ADDRESS, int PORT) {
        this(ADDRESS, PORT, 1000);
    }

    public ClientHandler(String ADDRESS, int PORT, long TIMEOUT) {
        this.ADDRESS = ADDRESS;
        this.PORT = PORT;
        this.TIMEOUT = TIMEOUT;
    }

    public SelectionKey getSelectionKey() {
            while (selectionKey == null);
            return selectionKey;
    }

    @Override
    public void run() {
        SocketChannel channel;
        try {
            selector = Selector.open();
            channel = SocketChannel.open();
            channel.configureBlocking(false);

            symmetricKey = SymmetricUtil.nextSymmetricKey();

            selectionKey = channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(new InetSocketAddress(ADDRESS, PORT));

            while (!Thread.interrupted()) {

                selector.select(TIMEOUT);

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

    protected abstract void connect(SelectionKey key) throws IOException;

    protected abstract void write(SelectionKey key) throws IOException;

    protected abstract void read(SelectionKey key) throws IOException;

    private void close() {
        try {
            selector.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
