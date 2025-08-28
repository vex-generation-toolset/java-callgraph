/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.logger;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * OpenRefactory daemon.
 * 
 * @author Munawar Hafiz
 */
public class ORDaemonLogger implements ILogger {
    private final String LOG_FILE = "." + File.separator + "log_ordaemon";

    private Logger logger;

    public ORDaemonLogger() {
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileHandler fileHandler = null;
        ConsoleHandler consoleHandler = null;
        try {
            fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            consoleHandler = new ConsoleHandler();
            logger = Logger.getLogger("ORDaemon");
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            TimestampFormatter formatter = new TimestampFormatter();
            for (Handler h : logger.getHandlers()) {
                h.setFormatter(formatter);
            }
            logger.log(Level.INFO, System.lineSeparator() + "STARTING LOGGER" + System.lineSeparator());
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void log(String message) {
        logger.log(Level.INFO, message);
    }

    class TimestampFormatter extends Formatter {
        private Date date;
        private DateFormat df;

        public TimestampFormatter() {
            date = new Date();
            df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("PST"));
        }

        @Override
        public String format(LogRecord record) {
            date.setTime(record.getMillis());
            StringBuilder sb = new StringBuilder();
            sb.append(df.format(date)).append(" PST ");
            if (record.getLevel().equals(Level.SEVERE)) {
                sb.append("[ERROR]: ");
            } else {
                if (!record.getLevel().equals(Level.INFO)) {
                    sb.append("[").append(record.getLevel()).append("]: ");
                }
            }
            sb.append(record.getMessage()).append(System.lineSeparator());
            return sb.toString();
        }
    }
}
