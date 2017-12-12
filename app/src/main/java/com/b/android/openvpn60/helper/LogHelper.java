package com.b.android.openvpn60.helper;

import android.content.Context;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by b on 12/12/2017.
 */

public class LogHelper {
    private Logger logger;


    public LogHelper(Context context) {
        logger = Logger.getLogger(context.getClass().toString());
    }

    public void logException(Throwable throwable) {
        logger.log(Level.SEVERE, "EXCEPTION! ", throwable);
    }


    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }
}
