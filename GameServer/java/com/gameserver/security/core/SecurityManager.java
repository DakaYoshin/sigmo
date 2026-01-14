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

import java.util.logging.Logger;

import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.security.antidupe.AntiDupeGuard;
import com.gameserver.security.audit.SecurityAudit;
import com.gameserver.security.detector.BotDetector;
import com.gameserver.security.firewall.PacketFirewall;
import com.gameserver.security.validator.ActionValidator;

/**
 * Main security system coordinator.
 * Initializes and manages all security subsystems.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class SecurityManager {

    private static final Logger _log = Logger.getLogger(SecurityManager.class.getName());
    private static SecurityManager _instance;

    private volatile boolean _initialized;

    /**
     * Private constructor.
     */
    private SecurityManager() {
        _initialized = false;
    }

    /**
     * Get singleton instance.
     * 
     * @return SecurityManager instance
     */
    public static SecurityManager getInstance() {
        if (_instance == null) {
            synchronized (SecurityManager.class) {
                if (_instance == null) {
                    _instance = new SecurityManager();
                }
            }
        }
        return _instance;
    }

    /**
     * Initialize security system.
     */
    public void initialize() {
        if (_initialized) {
            _log.warning("SecurityManager already initialized!");
            return;
        }

        _log.info("=================================================");
        _log.info("  SIGMO Security System v1.0.0");
        _log.info("  Initializing security subsystems...");
        _log.info("=================================================");

        try {
            // Load configuration
            _log.info("Loading security configuration...");
            SecurityConfig.load();

            // Initialize subsystems
            _log.info("Initializing SecurityLogger...");
            SecurityLogger.getInstance();

            _log.info("Initializing AntiDupeGuard...");
            AntiDupeGuard.getInstance();

            _log.info("Initializing PacketFirewall...");
            PacketFirewall.getInstance();

            _log.info("Initializing ActionValidator...");
            ActionValidator.getInstance();

            _log.info("Initializing BotDetector...");
            BotDetector.getInstance();

            _log.info("Initializing SecurityAudit...");
            SecurityAudit.getInstance();

            _initialized = true;

            _log.info("=================================================");
            _log.info("  Security System Initialized Successfully");
            _log.info("=================================================");

            // Print status
            printStatus();

        } catch (Exception e) {
            _log.severe("Failed to initialize SecurityManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown security system.
     */
    public void shutdown() {
        if (!_initialized) {
            return;
        }

        _log.info("Shutting down security system...");

        try {
            SecurityAudit.getInstance().shutdown();
            SecurityLogger.getInstance().shutdown();

            _initialized = false;
            _log.info("Security system shutdown complete.");

        } catch (Exception e) {
            _log.warning("Error during security system shutdown: " + e.getMessage());
        }
    }

    /**
     * Handle player login.
     * 
     * @param player Player logging in
     */
    public void onPlayerLogin(L2PcInstance player) {
        if (!_initialized || player == null) {
            return;
        }

        // Initialize tracking for this player
        BotDetector.getInstance().trackAction(player);
    }

    /**
     * Handle player logout.
     * 
     * @param player Player logging out
     */
    public void onPlayerLogout(L2PcInstance player) {
        if (!_initialized || player == null) {
            return;
        }

        // Cleanup player data
        AntiDupeGuard.getInstance().releaseAllLocks(player);
        ActionValidator.getInstance().removePlayer(player);
        BotDetector.getInstance().removePlayer(player);
    }

    /**
     * Handle player disconnect.
     * 
     * @param player Player disconnecting
     */
    public void onPlayerDisconnect(L2PcInstance player) {
        onPlayerLogout(player);
    }

    /**
     * Print security system status.
     */
    public void printStatus() {
        _log.info("=================================================");
        _log.info("  Security System Status");
        _log.info("=================================================");
        _log.info("Anti-Dupe Guard: " + (SecurityConfig.ANTIDUPE_ENABLED ? "ENABLED" : "DISABLED"));
        _log.info("  " + AntiDupeGuard.getInstance().getStats());
        _log.info("");
        _log.info("Packet Firewall: " + (SecurityConfig.FIREWALL_ENABLED ? "ENABLED" : "DISABLED"));
        _log.info("  " + PacketFirewall.getInstance().getStats());
        _log.info("");
        _log.info("Action Validator: " + (SecurityConfig.VALIDATOR_ENABLED ? "ENABLED" : "DISABLED"));
        _log.info("  " + ActionValidator.getInstance().getStats());
        _log.info("");
        _log.info("Bot Detector: " + (SecurityConfig.BOTDETECTOR_ENABLED ? "ENABLED" : "DISABLED"));
        _log.info("  " + BotDetector.getInstance().getStats());
        _log.info("");
        _log.info("Security Audit: " + (SecurityConfig.AUDIT_ENABLED ? "ENABLED" : "DISABLED"));
        _log.info("  " + SecurityAudit.getInstance().getStats());
        _log.info("  SecurityLogger Queue: " + SecurityLogger.getInstance().getQueueSize());
        _log.info("=================================================");
    }

    /**
     * Get system statistics.
     * 
     * @return Statistics string
     */
    public String getStats() {
        if (!_initialized) {
            return "Security system not initialized";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("=== Security System Statistics ===\n");
        sb.append(AntiDupeGuard.getInstance().getStats()).append("\n");
        sb.append(PacketFirewall.getInstance().getStats()).append("\n");
        sb.append(ActionValidator.getInstance().getStats()).append("\n");
        sb.append(BotDetector.getInstance().getStats()).append("\n");
        sb.append(SecurityAudit.getInstance().getStats()).append("\n");
        sb.append("SecurityLogger Queue: ").append(SecurityLogger.getInstance().getQueueSize());

        return sb.toString();
    }

    /**
     * Check if security system is initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return _initialized;
    }
}
