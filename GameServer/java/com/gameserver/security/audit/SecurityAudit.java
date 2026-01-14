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
package com.gameserver.security.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.security.core.SecurityConfig;
import com.gameserver.security.core.SecurityLogger;
import com.util.database.L2DatabaseFactory;

/**
 * Security audit system.
 * Logs all critical security events to database and file.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class SecurityAudit implements Runnable {

    private static final Logger _log = Logger.getLogger(SecurityAudit.class.getName());
    private static SecurityAudit _instance;

    // Audit event queue
    private final BlockingQueue<AuditEvent> _eventQueue;

    // Writer thread
    private final Thread _writerThread;
    private volatile boolean _running;

    /**
     * Audit event severity levels.
     */
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * Audit event container.
     */
    private static class AuditEvent {
        final String eventType;
        final int playerId;
        final String playerName;
        final String accountName;
        final String ipAddress;
        final String details;
        final Severity severity;
        @SuppressWarnings("unused")
        final long timestamp;

        AuditEvent(String eventType, int playerId, String playerName, String accountName,
                String ipAddress, String details, Severity severity) {
            this.eventType = eventType;
            this.playerId = playerId;
            this.playerName = playerName;
            this.accountName = accountName;
            this.ipAddress = ipAddress;
            this.details = details;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Private constructor.
     */
    private SecurityAudit() {
        _eventQueue = new LinkedBlockingQueue<>(10000);
        _running = true;
        _writerThread = new Thread(this, "SecurityAudit");
        _writerThread.setDaemon(true);
        _writerThread.start();

        _log.info("SecurityAudit initialized.");
    }

    /**
     * Get singleton instance.
     * 
     * @return SecurityAudit instance
     */
    public static SecurityAudit getInstance() {
        if (_instance == null) {
            synchronized (SecurityAudit.class) {
                if (_instance == null) {
                    _instance = new SecurityAudit();
                }
            }
        }
        return _instance;
    }

    /**
     * Log item creation.
     * 
     * @param player Player who created item
     * @param itemId Item ID
     * @param count  Item count
     * @param method Creation method (ADMIN, GM_COMMAND, etc.)
     */
    public void logItemCreation(L2PcInstance player, int itemId, long count, String method) {
        if (!SecurityConfig.AUDIT_ENABLED) {
            return;
        }

        final String details = String.format("ItemId=%d Count=%d Method=%s", itemId, count, method);
        logEvent("ITEM_CREATION", player, details, Severity.WARNING);
    }

    /**
     * Log item deletion.
     * 
     * @param player Player who deleted item
     * @param itemId Item ID
     * @param count  Item count
     * @param method Deletion method
     */
    public void logItemDeletion(L2PcInstance player, int itemId, long count, String method) {
        if (!SecurityConfig.AUDIT_ENABLED) {
            return;
        }

        final String details = String.format("ItemId=%d Count=%d Method=%s", itemId, count, method);
        logEvent("ITEM_DELETION", player, details, Severity.WARNING);
    }

    /**
     * Log GM command.
     * 
     * @param player  GM player
     * @param command Command executed
     * @param target  Target player (if any)
     */
    public void logGMCommand(L2PcInstance player, String command, String target) {
        if (!SecurityConfig.AUDIT_ENABLED || !SecurityConfig.AUDIT_LOG_GM_COMMANDS) {
            return;
        }

        final String details = String.format("Command=%s Target=%s", command, target != null ? target : "NONE");
        logEvent("GM_COMMAND", player, details, Severity.INFO);
    }

    /**
     * Log trade transaction.
     * 
     * @param player1     First player
     * @param player2     Second player
     * @param adenaAmount Adena amount
     */
    public void logTrade(L2PcInstance player1, L2PcInstance player2, long adenaAmount) {
        if (!SecurityConfig.AUDIT_ENABLED || !SecurityConfig.AUDIT_LOG_TRADES) {
            return;
        }

        if (adenaAmount >= SecurityConfig.AUDIT_TRADE_THRESHOLD) {
            final String details = String.format("Partner=%s[%d] Adena=%d",
                    player2.getName(), player2.getObjectId(), adenaAmount);
            logEvent("TRADE", player1, details, Severity.INFO);
        }
    }

    /**
     * Log warehouse transaction.
     * 
     * @param player      Player
     * @param operation   Operation type (DEPOSIT/WITHDRAW)
     * @param adenaAmount Adena amount
     */
    public void logWarehouse(L2PcInstance player, String operation, long adenaAmount) {
        if (!SecurityConfig.AUDIT_ENABLED || !SecurityConfig.AUDIT_LOG_WAREHOUSE) {
            return;
        }

        if (adenaAmount >= SecurityConfig.AUDIT_WAREHOUSE_THRESHOLD) {
            final String details = String.format("Operation=%s Adena=%d", operation, adenaAmount);
            logEvent("WAREHOUSE", player, details, Severity.INFO);
        }
    }

    /**
     * Log security violation.
     * 
     * @param player        Player
     * @param violationType Type of violation
     * @param details       Violation details
     */
    public void logViolation(L2PcInstance player, String violationType, String details) {
        if (!SecurityConfig.AUDIT_ENABLED) {
            return;
        }

        final String fullDetails = String.format("Type=%s Details=%s", violationType, details);
        logEvent("SECURITY_VIOLATION", player, fullDetails, Severity.CRITICAL);
    }

    /**
     * Log ban action.
     * 
     * @param player   Player being banned
     * @param admin    Admin performing ban
     * @param reason   Ban reason
     * @param duration Ban duration
     */
    public void logBan(L2PcInstance player, String admin, String reason, long duration) {
        if (!SecurityConfig.AUDIT_ENABLED) {
            return;
        }

        final String details = String.format("Admin=%s Reason=%s Duration=%d", admin, reason, duration);
        logEvent("BAN", player, details, Severity.CRITICAL);
    }

    /**
     * Log generic event.
     * 
     * @param eventType Event type
     * @param player    Player
     * @param details   Event details
     * @param severity  Severity level
     */
    private void logEvent(String eventType, L2PcInstance player, String details, Severity severity) {
        if (player == null) {
            return;
        }

        final String accountName = player.getAccountName();
        final String ipAddress = player.getClient() != null
                ? player.getClient().getConnection().getInetAddress().getHostAddress()
                : "UNKNOWN";

        final AuditEvent event = new AuditEvent(
                eventType,
                player.getObjectId(),
                player.getName(),
                accountName,
                ipAddress,
                details,
                severity);

        if (!_eventQueue.offer(event)) {
            _log.warning("Audit event queue is full! Event dropped: " + eventType);
        }

        // Also log to file
        SecurityLogger.getInstance().logf("AUDIT",
                "%s: Player=%s[%d] Account=%s IP=%s Details=%s",
                eventType, player.getName(), player.getObjectId(), accountName, ipAddress, details);
    }

    /**
     * Writer thread main loop.
     */
    @Override
    public void run() {
        _log.info("SecurityAudit writer thread started.");

        while (_running || !_eventQueue.isEmpty()) {
            try {
                final AuditEvent event = _eventQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);

                if (event != null && SecurityConfig.AUDIT_LOG_TO_DATABASE) {
                    writeToDatabase(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                _log.warning("Error in SecurityAudit writer thread: " + e.getMessage());
            }
        }

        _log.info("SecurityAudit writer thread stopped.");
    }

    /**
     * Write audit event to database.
     * 
     * @param event Audit event
     */
    private void writeToDatabase(AuditEvent event) {
        final String sql = "INSERT INTO security_audit " +
                "(event_type, player_id, player_name, account_name, ip_address, details, severity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, event.eventType);
            ps.setInt(2, event.playerId);
            ps.setString(3, event.playerName);
            ps.setString(4, event.accountName);
            ps.setString(5, event.ipAddress);
            ps.setString(6, event.details);
            ps.setString(7, event.severity.name());

            ps.executeUpdate();

        } catch (SQLException e) {
            _log.warning("Failed to write audit event to database: " + e.getMessage());
        }
    }

    /**
     * Shutdown the audit system.
     */
    public void shutdown() {
        _log.info("Shutting down SecurityAudit...");
        _running = false;

        try {
            _writerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        _log.info("SecurityAudit shutdown complete. Pending events: " + _eventQueue.size());
    }

    /**
     * Get queue size for monitoring.
     * 
     * @return Current queue size
     */
    public int getQueueSize() {
        return _eventQueue.size();
    }

    /**
     * Get statistics.
     * 
     * @return Statistics string
     */
    public String getStats() {
        return String.format("SecurityAudit Stats - Pending Events: %d", _eventQueue.size());
    }
}
