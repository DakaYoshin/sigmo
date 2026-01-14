/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.gameserver.security.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Centralized security logging system.
 * Provides async file logging to prevent performance impact.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class SecurityLogger implements Runnable {

    private static final Logger _log = Logger.getLogger(SecurityLogger.class.getName());
    private static final String LOG_DIR = "./log/security/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static SecurityLogger _instance;

    private final BlockingQueue<LogEntry> _logQueue;
    private final Thread _writerThread;
    private volatile boolean _running;

    /**
     * Log entry container.
     */
    private static class LogEntry {
        final String category;
        final String message;
        final long timestamp;

        LogEntry(String category, String message) {
            this.category = category;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Private constructor.
     */
    private SecurityLogger() {
        _logQueue = new LinkedBlockingQueue<>(10000);
        _running = true;
        _writerThread = new Thread(this, "SecurityLogger");
        _writerThread.setDaemon(true);
        _writerThread.start();

        // Ensure log directory exists
        final File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        _log.info("SecurityLogger initialized.");
    }

    /**
     * Get singleton instance.
     * 
     * @return SecurityLogger instance
     */
    public static SecurityLogger getInstance() {
        if (_instance == null) {
            synchronized (SecurityLogger.class) {
                if (_instance == null) {
                    _instance = new SecurityLogger();
                }
            }
        }
        return _instance;
    }

    /**
     * Log a security event.
     * 
     * @param category Event category (e.g., "ANTIDUPE", "FIREWALL", "BOT")
     * @param message  Log message
     */
    public void log(String category, String message) {
        if (!SecurityConfig.AUDIT_LOG_TO_FILE) {
            return;
        }

        final LogEntry entry = new LogEntry(category, message);

        if (!_logQueue.offer(entry)) {
            _log.warning("Security log queue is full! Message dropped: " + message);
        }
    }

    /**
     * Log with formatted message.
     * 
     * @param category Event category
     * @param format   Message format
     * @param args     Format arguments
     */
    public void logf(String category, String format, Object... args) {
        log(category, String.format(format, args));
    }

    /**
     * Writer thread main loop.
     */
    @Override
    public void run() {
        _log.info("SecurityLogger writer thread started.");

        while (_running || !_logQueue.isEmpty()) {
            try {
                final LogEntry entry = _logQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);

                if (entry != null) {
                    writeToFile(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                _log.warning("Error in SecurityLogger writer thread: " + e.getMessage());
            }
        }

        _log.info("SecurityLogger writer thread stopped.");
    }

    /**
     * Write log entry to file.
     * 
     * @param entry Log entry
     */
    private void writeToFile(LogEntry entry) {
        final String fileName = LOG_DIR + entry.category.toLowerCase() + "_" +
                FILE_DATE_FORMAT.format(new Date(entry.timestamp)) + ".log";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            final String timestamp = DATE_FORMAT.format(new Date(entry.timestamp));
            writer.write(String.format("[%s] %s%n", timestamp, entry.message));
        } catch (IOException e) {
            _log.warning("Failed to write security log: " + e.getMessage());
        }
    }

    /**
     * Shutdown the logger.
     */
    public void shutdown() {
        _log.info("Shutting down SecurityLogger...");
        _running = false;

        try {
            _writerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        _log.info("SecurityLogger shutdown complete. Pending logs: " + _logQueue.size());
    }

    /**
     * Get queue size for monitoring.
     * 
     * @return Current queue size
     */
    public int getQueueSize() {
        return _logQueue.size();
    }
}
