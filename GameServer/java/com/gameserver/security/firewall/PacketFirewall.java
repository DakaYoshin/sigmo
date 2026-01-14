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
package com.gameserver.security.firewall;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.gameserver.network.L2GameClient;
import com.gameserver.security.core.SecurityConfig;
import com.gameserver.security.core.SecurityLogger;
import com.gameserver.thread.LoginServerThread;

/**
 * Packet firewall system.
 * Validates packet rates, sequences, and sizes to prevent injection attacks.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class PacketFirewall {

    private static final Logger _log = Logger.getLogger(PacketFirewall.class.getName());
    private static PacketFirewall _instance;

    // Client packet statistics: clientId -> ClientPacketStats
    private final ConcurrentHashMap<String, ClientPacketStats> _clientStats;

    // Packet opcode categories
    private static final int[] MOVEMENT_OPCODES = { 0x01, 0x48, 0x59 }; // MoveBackwardToLocation, ValidatePosition,
                                                                        // etc.
    private static final int[] ACTION_OPCODES = { 0x04, 0x0A, 0x2F }; // Action, AttackRequest, UseItem, etc.
    private static final int[] TRADE_OPCODES = { 0x15, 0x16, 0x17 }; // TradeRequest, AddTradeItem, TradeDone

    /**
     * Client packet statistics.
     */
    private static class ClientPacketStats {
        final AtomicLong generalPackets;
        final AtomicLong movementPackets;
        final AtomicLong actionPackets;
        final AtomicLong tradePackets;
        final AtomicInteger violations;
        final AtomicInteger punishmentLevel;
        volatile long lastResetTime;
        volatile long lastViolationTime;
        volatile int lastOpcode;

        ClientPacketStats(String clientId) {
            // clientId is tracked via the map key, not stored here
            this.generalPackets = new AtomicLong(0);
            this.movementPackets = new AtomicLong(0);
            this.actionPackets = new AtomicLong(0);
            this.tradePackets = new AtomicLong(0);
            this.violations = new AtomicInteger(0);
            this.punishmentLevel = new AtomicInteger(0);
            this.lastResetTime = System.currentTimeMillis();
            this.lastViolationTime = 0;
            this.lastOpcode = -1;
        }

        void reset() {
            generalPackets.set(0);
            movementPackets.set(0);
            actionPackets.set(0);
            tradePackets.set(0);
            lastResetTime = System.currentTimeMillis();
        }
    }

    /**
     * Private constructor.
     */
    private PacketFirewall() {
        _clientStats = new ConcurrentHashMap<>();

        // Start reset task
        startResetTask();

        _log.info("PacketFirewall initialized.");
    }

    /**
     * Get singleton instance.
     * 
     * @return PacketFirewall instance
     */
    public static PacketFirewall getInstance() {
        if (_instance == null) {
            synchronized (PacketFirewall.class) {
                if (_instance == null) {
                    _instance = new PacketFirewall();
                }
            }
        }
        return _instance;
    }

    /**
     * Validate packet.
     * 
     * @param client Game client
     * @param opcode Packet opcode
     * @param size   Packet size
     * @return true if packet is valid
     */
    public boolean validatePacket(L2GameClient client, int opcode, int size) {
        if (!SecurityConfig.FIREWALL_ENABLED) {
            return true;
        }

        if (client == null) {
            return false;
        }

        final String clientId = getClientId(client);
        final ClientPacketStats stats = _clientStats.computeIfAbsent(clientId, ClientPacketStats::new);

        // Check if we need to reset counters (every second)
        final long now = System.currentTimeMillis();
        if (now - stats.lastResetTime >= 1000) {
            stats.reset();
        }

        // Categorize and count packet
        final PacketCategory category = categorizePacket(opcode);
        final long count = incrementPacketCount(stats, category);

        // Check rate limits
        if (!checkRateLimit(client, stats, category, count, opcode)) {
            return false;
        }

        // Check packet size anomalies
        if (!checkPacketSize(client, stats, opcode, size)) {
            return false;
        }

        // Check packet sequence
        if (!checkPacketSequence(client, stats, opcode)) {
            return false;
        }

        stats.lastOpcode = opcode;
        return true;
    }

    /**
     * Categorize packet by opcode.
     * 
     * @param opcode Packet opcode
     * @return Packet category
     */
    private PacketCategory categorizePacket(int opcode) {
        for (int moveOp : MOVEMENT_OPCODES) {
            if (opcode == moveOp) {
                return PacketCategory.MOVEMENT;
            }
        }

        for (int actionOp : ACTION_OPCODES) {
            if (opcode == actionOp) {
                return PacketCategory.ACTION;
            }
        }

        for (int tradeOp : TRADE_OPCODES) {
            if (opcode == tradeOp) {
                return PacketCategory.TRADE;
            }
        }

        return PacketCategory.GENERAL;
    }

    /**
     * Increment packet count for category.
     * 
     * @param stats    Client stats
     * @param category Packet category
     * @return New count
     */
    private long incrementPacketCount(ClientPacketStats stats, PacketCategory category) {
        return switch (category) {
            case MOVEMENT -> stats.movementPackets.incrementAndGet();
            case ACTION -> stats.actionPackets.incrementAndGet();
            case TRADE -> stats.tradePackets.incrementAndGet();
            default -> stats.generalPackets.incrementAndGet();
        };
    }

    /**
     * Check rate limit for packet category.
     * 
     * @param client   Game client
     * @param stats    Client stats
     * @param category Packet category
     * @param count    Current count
     * @param opcode   Packet opcode
     * @return true if within limits
     */
    private boolean checkRateLimit(L2GameClient client, ClientPacketStats stats,
            PacketCategory category, long count, int opcode) {
        final int limit = switch (category) {
            case MOVEMENT -> SecurityConfig.FIREWALL_RATE_MOVEMENT;
            case ACTION -> SecurityConfig.FIREWALL_RATE_ACTION;
            case TRADE -> SecurityConfig.FIREWALL_RATE_TRADE;
            default -> SecurityConfig.FIREWALL_RATE_GENERAL;
        };

        if (count > limit) {
            handleViolation(client, stats, "RATE_LIMIT",
                    String.format("Category=%s Opcode=0x%02X Count=%d Limit=%d",
                            category, opcode, count, limit));
            return false;
        }

        return true;
    }

    /**
     * Check packet size for anomalies.
     * 
     * @param client Game client
     * @param stats  Client stats
     * @param opcode Packet opcode
     * @param size   Packet size
     * @return true if size is valid
     */
    private boolean checkPacketSize(L2GameClient client, ClientPacketStats stats,
            int opcode, int size) {
        // Define expected size ranges for common packets
        // This is a simplified check - expand based on actual packet definitions

        if (size < 0 || size > 65535) {
            handleViolation(client, stats, "INVALID_SIZE",
                    String.format("Opcode=0x%02X Size=%d", opcode, size));
            return false;
        }

        // Check for suspiciously large packets
        if (size > 8192) {
            _log.warning(String.format("Large packet detected: Client=%s Opcode=0x%02X Size=%d",
                    getClientId(client), opcode, size));
            SecurityLogger.getInstance().logf("FIREWALL",
                    "LARGE_PACKET: Client=%s Opcode=0x%02X Size=%d",
                    getClientId(client), opcode, size);
        }

        return true;
    }

    /**
     * Check packet sequence for anomalies.
     * 
     * @param client Game client
     * @param stats  Client stats
     * @param opcode Current opcode
     * @return true if sequence is valid
     */
    private boolean checkPacketSequence(L2GameClient client, ClientPacketStats stats, int opcode) {
        // Check for suspicious packet sequences
        // For example, trade packets should follow a specific order

        if (stats.lastOpcode == opcode && isRepeatSensitiveOpcode(opcode)) {
            final long now = System.currentTimeMillis();
            if (now - stats.lastResetTime < 100) { // Same packet twice within 100ms
                handleViolation(client, stats, "RAPID_REPEAT",
                        String.format("Opcode=0x%02X Interval=%dms",
                                opcode, now - stats.lastResetTime));
                return false;
            }
        }

        return true;
    }

    /**
     * Check if opcode is sensitive to rapid repetition.
     * 
     * @param opcode Packet opcode
     * @return true if sensitive
     */
    private boolean isRepeatSensitiveOpcode(int opcode) {
        // Trade, drop, destroy, etc.
        for (int tradeOp : TRADE_OPCODES) {
            if (opcode == tradeOp) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle packet violation.
     * 
     * @param client        Game client
     * @param stats         Client stats
     * @param violationType Type of violation
     * @param details       Violation details
     */
    private void handleViolation(L2GameClient client, ClientPacketStats stats,
            String violationType, String details) {
        final int violations = stats.violations.incrementAndGet();
        stats.lastViolationTime = System.currentTimeMillis();

        SecurityLogger.getInstance().logf("FIREWALL",
                "VIOLATION: Client=%s Type=%s Details=%s TotalViolations=%d",
                getClientId(client), violationType, details, violations);

        if (SecurityConfig.FIREWALL_AUTO_PUNISH) {
            applyPunishment(client, stats, violations);
        }
    }

    /**
     * Apply punishment based on violation count.
     * 
     * @param client     Game client
     * @param stats      Client stats
     * @param violations Total violations
     */
    private void applyPunishment(L2GameClient client, ClientPacketStats stats, int violations) {
        final long now = System.currentTimeMillis();
        final long timeSinceLastViolation = now - stats.lastViolationTime;

        // Reset violation count if last violation was more than 1 minute ago
        if (timeSinceLastViolation > 60000) {
            stats.violations.set(1);
            return;
        }

        // Progressive punishment
        if (violations >= SecurityConfig.FIREWALL_BAN_THRESHOLD) {
            // Ban
            _log.warning(String.format("BANNING client %s for excessive packet violations (%d)",
                    getClientId(client), violations));
            SecurityLogger.getInstance().logf("FIREWALL",
                    "BAN: Client=%s Violations=%d", getClientId(client), violations);
            stats.punishmentLevel.set(3);

            // Ban the account if player is logged in
            if (client.getActiveChar() != null) {
                try {
                    // Ban through LoginServerThread
                    LoginServerThread.getInstance().sendAccessLevel(
                            client.getAccountName(), -100);
                    _log.info(String.format("Account %s banned for packet violations",
                            client.getAccountName()));
                } catch (Exception e) {
                    _log.warning("Failed to ban account through login server: " + e.getMessage());
                }
            }

            client.close(null);

        } else if (violations >= SecurityConfig.FIREWALL_KICK_THRESHOLD) {
            // Kick
            _log.warning(String.format("KICKING client %s for packet violations (%d)",
                    getClientId(client), violations));
            SecurityLogger.getInstance().logf("FIREWALL",
                    "KICK: Client=%s Violations=%d", getClientId(client), violations);
            stats.punishmentLevel.set(2);
            client.close(null);

        } else if (violations >= SecurityConfig.FIREWALL_THROTTLE_THRESHOLD) {
            // Throttle (delay packet processing)
            stats.punishmentLevel.set(1);
            try {
                Thread.sleep(100); // 100ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } else if (violations >= SecurityConfig.FIREWALL_WARN_THRESHOLD) {
            // Warn
            stats.punishmentLevel.set(0);
            if (client.getActiveChar() != null) {
                client.getActiveChar().sendMessage("Warning: Suspicious packet activity detected.");
            }
        }
    }

    /**
     * Get client identifier.
     * 
     * @param client Game client
     * @return Client ID string
     */
    private String getClientId(L2GameClient client) {
        if (client.getActiveChar() != null) {
            return client.getActiveChar().getName() + "[" + client.getActiveChar().getObjectId() + "]";
        }
        return client.toString();
    }

    /**
     * Remove client statistics (on disconnect).
     * 
     * @param client Game client
     */
    public void removeClient(L2GameClient client) {
        if (client != null) {
            _clientStats.remove(getClientId(client));
        }
    }

    /**
     * Start periodic reset task.
     */
    private void startResetTask() {
        final Thread resetThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(300000); // Every 5 minutes
                    cleanupOldStats();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PacketFirewall-Reset");

        resetThread.setDaemon(true);
        resetThread.start();
    }

    /**
     * Cleanup old client statistics.
     */
    private void cleanupOldStats() {
        final long now = System.currentTimeMillis();
        final long timeout = 300000; // 5 minutes

        int removed = 0;

        for (var entry : _clientStats.entrySet()) {
            final ClientPacketStats stats = entry.getValue();

            if (now - stats.lastResetTime > timeout) {
                _clientStats.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            _log.info(String.format("Cleaned up %d old client packet statistics", removed));
        }
    }

    /**
     * Get statistics.
     * 
     * @return Statistics string
     */
    public String getStats() {
        return String.format("PacketFirewall Stats - Monitored Clients: %d", _clientStats.size());
    }

    /**
     * Packet category enum.
     */
    private enum PacketCategory {
        GENERAL,
        MOVEMENT,
        ACTION,
        TRADE
    }
}
