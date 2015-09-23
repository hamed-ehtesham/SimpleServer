import javax.crypto.SealedObject;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

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

    public static <T> T readObject(byte[] data, Class<T> outputClass) throws IOException {
        T output = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        try {
            ArrayList<T> outputWrapper = (ArrayList<T>) ois.readObject();
            output = outputWrapper.get(0);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static<T> void writeObject(SocketChannel channel, T data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        ArrayList<T> dataWrapper = new ArrayList<T>();
        dataWrapper.add(data);
        oos.writeObject(dataWrapper);
        oos.flush();

        ByteBuffer buffer = ByteBuffer.wrap(bos.toByteArray());
        channel.write(buffer);
    }
}
