package util;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 14.10.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class MyUtils {



    /**
     *  From http://stackoverflow.com/questions/10578674/find-hour-or-minute-difference-between-2-java-sql-timestamps
     *
     * @param currentTime
     * @param oldTime
     * @return Difference between given timestamps in milliseconds.
     */
    public static long compareTwoTimeStamps(java.sql.Timestamp currentTime, java.sql.Timestamp oldTime)
    {
        long milliseconds1 = oldTime.getTime();
        long milliseconds2 = currentTime.getTime();

        long diff = milliseconds2 - milliseconds1;

        return diff;
    }
}
