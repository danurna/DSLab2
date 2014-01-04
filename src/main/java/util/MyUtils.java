package util;

import java.io.*;
import java.security.PrivateKey;
import java.security.Security;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import java.security.PublicKey;
import org.bouncycastle.openssl.PEMReader;
import java.security.KeyPair;
import org.bouncycastle.openssl.PasswordFinder;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 14.10.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class MyUtils {

    /**
     * From http://stackoverflow.com/questions/10578674/find-hour-or-minute-difference-between-2-java-sql-timestamps
     *
     * @param currentTime
     * @param oldTime
     * @return Difference between given timestamps in milliseconds.
     */
    public static long compareTwoTimeStamps(Timestamp currentTime, Timestamp oldTime) {
        long milliseconds1 = oldTime.getTime();
        long milliseconds2 = currentTime.getTime();

        long diff = milliseconds2 - milliseconds1;

        return diff;
    }


    /**
     * Returns current timestamp.
     *
     * @return timestamp
     */
    public static Timestamp getCurrentTimestamp() {
        java.util.Date date = new java.util.Date();
        return new Timestamp(date.getTime());
    }

    //From http://stackoverflow.com/questions/858980/file-to-byte-in-java
    public static byte[] convertFileToByteArray(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if (inputStream.read(buffer) == -1) {
                throw new IOException("EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return buffer;
    }

    public static void saveByteArrayAsFile(byte[] file, String path) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(path);

        try {
            outputStream.write(file);
        } finally {
            outputStream.close();
        }
    }


    /**
     * Reads files from directory and put the names into a set of strings.
     *
     * @return Set of filenames inside the fs's directory.
     */
    public static Set<String> getFileNamesInDirectory(String dir) {
        File file = new File(dir);
        Set<String> files = new HashSet<String>();
        files.addAll(Arrays.asList(file.list()));
        return files;
    }

    /**
     * Encodes data base64
     * @param data - data to encode
     * @return base64 encoded data.
     */
    public static byte[] base64encodeBytes(byte[] data){
        return Base64.encode(data);
    }

    /**
     * Decodes base64-encoded data.
     * @param data - base64-encoded data
     * @return decoded data.
     */
    public static byte[] base64decodeBytes(byte[] data){
        return Base64.decode(data);
    }

    /**
     * Read PEM formatted RSA public key.
     * @param path - Path to pem file
     * @return publicKey
     * @throws IOException
     */
    public static PublicKey getPublicKeyForPath(String path) throws IOException {
        PEMReader in = new PEMReader(new FileReader(path));
        PublicKey publicKey = (PublicKey) in.readObject();
        return publicKey;
    }

    /**
     * Read PEM formatted RSA private key.
     * @param path - Path to pem file
     * @return private key
     * @throws IOException
     */
    public static PrivateKey getPrivateKeyForPath(String path) throws IOException {

        PEMReader in = new PEMReader(new FileReader(path), new PasswordFinder() {

            @Override
            public char[] getPassword() {
                // reads the password from standard input for decrypting the private key
                System.out.println("Enter pass phrase:");
                try {
                    InputStreamReader isr = new InputStreamReader(System.in);
                    BufferedReader bf = new BufferedReader(isr);
                    return bf.readLine().toCharArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // in case of error return empty password
                 return "".toCharArray();
            }
        });
        KeyPair keyPair = (KeyPair) in.readObject();
        PrivateKey privateKey = keyPair.getPrivate();
        return privateKey;
    }

    /**
     * Read PEM formatted RSA private key. Development only!
     * @param path - Path to pem file
     * @param password - to prevent always typing the password we use this convenience param
     * @return private key
     * @throws IOException
     */
    public static PrivateKey getPrivateKeyForPathAndPassword(String path, final String password) throws IOException {

        PEMReader in = new PEMReader(new FileReader(path), new PasswordFinder() {
            @Override
            public char[] getPassword() {
                return password.toCharArray();
            }
        });

        KeyPair keyPair = (KeyPair) in.readObject();
        PrivateKey privateKey = keyPair.getPrivate();
        return privateKey;
    }



}
