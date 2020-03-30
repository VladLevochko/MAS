package ua.kpi;

import java.util.logging.Logger;

public class MyLog {
    private static Logger logger = Logger.getLogger("MyLogger");

    public static void log(String message) {
        logger.info(message);
    }
}
