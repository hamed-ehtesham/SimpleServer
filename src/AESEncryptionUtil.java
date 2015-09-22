import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Aes encryption
 */
public class AESEncryptionUtil {

    private SecretKeySpec secretKey;

    private String decryptedString;
    private String encryptedString;

    public AESEncryptionUtil(String secretKey) {
        this.setKey(secretKey);
    }

    public void setKey(String myKey) {


        MessageDigest sha = null;
        try {
            byte[] key = myKey.getBytes("UTF-8");
            System.out.println(key.length);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit
            System.out.println(key.length);
            System.out.println(new String(key, "UTF-8"));
            secretKey = new SecretKeySpec(key, "AES");


        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    public String getDecryptedString() {
        return decryptedString;
    }

    public void setDecryptedString(String decryptedString) {
        this.decryptedString = decryptedString;
    }

    public String getEncryptedString() {
        return encryptedString;
    }

    public void setEncryptedString(String encryptedString) {
        this.encryptedString = encryptedString;
    }

    public String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);


            setEncryptedString(Base64.encodeBase64String(cipher.doFinal(strToEncrypt.getBytes("UTF-8"))));

        } catch (Exception e) {

            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            setDecryptedString(new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt))));

        } catch (Exception e) {

            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    public ByteBuffer encrypt(ByteBuffer bufferToEncrypt) {
        ByteBuffer output = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);


            byte[] bytes = Base64.encodeBase64(cipher.doFinal(bufferToEncrypt.array()));

            output = ByteBuffer.wrap(bytes);
        } catch (Exception e) {

            System.out.println("Error while encrypting: " + e.toString());
        }
        return output;
    }

    public ByteBuffer decrypt(ByteBuffer bufferToDecrypt) {
        return ByteBuffer.wrap(decrypt(bufferToDecrypt.array()));
    }

    public byte[] decrypt(byte[] arrayToDecrypt) {
        byte[] output = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            output = cipher.doFinal(Base64.decodeBase64(arrayToDecrypt));
        } catch (Exception e) {

            System.out.println("Error while decrypting: " + e.toString());
        }
        return output;
    }

    public static void main(String args[]) {
        final String strToEncrypt = "My text to encrypt";
        final String strPssword = "encryptor key";
        AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(strPssword);
        aesEncryptionUtil.setKey(strPssword);

        aesEncryptionUtil.encrypt(strToEncrypt.trim());

        System.out.println("String to Encrypt: " + strToEncrypt);
        System.out.println("Encrypted: " + aesEncryptionUtil.getEncryptedString());

        final String strToDecrypt = aesEncryptionUtil.getEncryptedString();
        aesEncryptionUtil.decrypt(strToDecrypt.trim());

        System.out.println("String To Decrypt : " + strToDecrypt);
        System.out.println("Decrypted : " + aesEncryptionUtil.getDecryptedString());

    }

}