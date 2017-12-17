package com.b.android.openvpn60.helper;

import android.content.Context;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by b on 12/12/2017.
 */

public class LogHelper {
    private Logger logger;


    private LogHelper(Context context) {
        logger = Logger.getLogger(context.getClass().toString());
    }

    public static LogHelper getLogHelper(Context context) {
        return new LogHelper(context);
    }

    public void logException(Throwable throwable) {
        logger.log(Level.SEVERE, "EXCEPTION! ", throwable);
    }

    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    public void logWarning(String warning) {
        logger.log(Level.WARNING, warning);
    }

    public void logWarning(String warning, Throwable throwable) {
        logger.log(Level.WARNING, warning, throwable);
    }
}
