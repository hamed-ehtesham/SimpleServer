import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.SealedObject;

/**
 * Created by Mohammad Amin on 11/09/2015.
 */
public class RSAEncryptionUtil {
    private static String algorithm = "RSA";
    private static int keysize = 512;
    private static String xform = "RSA/ECB/PKCS1Padding";
    private String publicKey = null;
    private PrivateKey privateKey = null;

    public RSAEncryptionUtil() throws Exception {
        KeyPair keyPair = RSAEncryptionUtil.generateKeyPair();
        BASE64Encoder encoder = new BASE64Encoder();
        byte[] encoded = keyPair.getPublic().getEncoded();
        publicKey = encoder.encode(encoded);
        privateKey = keyPair.getPrivate();
    }

    public String getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static void setAlgorithm(String algorithm) {
        RSAEncryptionUtil.algorithm = algorithm;
    }

    public static void setKeysize(int keysize) {
        RSAEncryptionUtil.keysize = keysize;
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        kpg.initialize(keysize);
        KeyPair kp = kpg.generateKeyPair();
        return kp;
    }

    public static SealedObject encryptByPublicKey(String input, String key) throws Exception {
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] sigBytes2 = decoder.decodeBuffer(key);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes2);
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory keyFact = KeyFactory.getInstance("RSA", "BC");
        PublicKey pubKey = keyFact.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance(xform);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return (new SealedObject(input, cipher));
    }

    public static String decryptByPrivateKey(SealedObject input, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(xform);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return (String) input.getObject(cipher);
    }

    public static void main(String[] args) {
        try {
            RSAEncryptionUtil rsaEncryptionUtil = new RSAEncryptionUtil();
            String input = "this is a test string";
            SealedObject sealedObject = encryptByPublicKey(input, rsaEncryptionUtil.getPublicKey());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sealedObject);
            oos.flush();
            Channels.newChannel(System.out).write(ByteBuffer.wrap(bos.toByteArray()));
            System.out.println();

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            sealedObject = null;
            try {
                sealedObject = (SealedObject) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (sealedObject != null) {
                System.out.println(decryptByPrivateKey(sealedObject, (new RSAEncryptionUtil()).getPrivateKey()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}