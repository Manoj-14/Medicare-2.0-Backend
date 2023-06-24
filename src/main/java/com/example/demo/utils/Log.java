package com.example.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {
    public static final Logger logger = LoggerFactory.getLogger(Log.class);

    public static void INFO(String message){
        logger.info(message);
    }

    public static void DEBUG(String message){
        logger.debug(message);
    }

    public static void warn(String message){
        logger.warn(message);
    }

    public static void ERROR(String message){
        logger.error(message);
    }
}
