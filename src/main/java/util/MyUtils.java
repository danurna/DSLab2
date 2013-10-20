package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;

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
}
