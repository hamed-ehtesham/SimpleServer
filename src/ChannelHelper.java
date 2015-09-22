import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Hamed on 9/21/2015.
 */
public class ChannelHelper {
    public static ByteArrayOutputStream read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();
        int length;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        do {
            try {
                length = channel.read(readBuffer);
            } catch (IOException e) {
                System.out.println("Reading problem, closing connection");
                key.cancel();
                channel.close();
                throw e;
            }
            byte[] dst = new byte[length];
            readBuffer.flip();
            readBuffer.get(dst, 0, length);
            bos.write(dst);

            readBuffer.compact();
        } while (length > 0);
        return bos;
    }
}
