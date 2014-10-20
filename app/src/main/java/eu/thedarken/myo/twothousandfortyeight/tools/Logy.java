package eu.thedarken.myo.twothousandfortyeight.tools;

import android.util.Log;
import eu.thedarken.myo.twothousandfortyeight.BuildConfig;

/**
 * It's Logy!
 */
public class Logy {
    private static final int VERBOSE = 1;
    private static final int DEBUG = 2;
    private static final int NORMAL = 3;
    private static final int QUIET = 4;
    public static final int SILENT = 5;

    private static int loglevel = BuildConfig.DEBUG ? VERBOSE : NORMAL;

    public static void v(String c, String s) {
        if (loglevel <= VERBOSE) {
            if (s == null)
                s = "\"NULL\"";
            Log.v(c, s);
        }
    }

    public static void d(String c, String s) {
        if (loglevel <= DEBUG) {
            if (s == null)
                s = "\"NULL\"";
            Log.d(c, s);
        }
    }

    public static void i(String c, String s) {
        if (loglevel <= NORMAL) {
            if (s == null)
                s = "\"NULL\"";
            Log.i(c, s);
        }
    }

    public static void w(String c, String s) {
        if (loglevel <= QUIET) {
            if (s == null)
                s = "\"NULL\"";
            Log.w(c, s);
        }
    }

    public static void e(String c, String s) {
        if (loglevel <= QUIET) {
            if (s == null)
                s = "\"NULL\"";
            Log.e(c, s);
        }
    }
}

