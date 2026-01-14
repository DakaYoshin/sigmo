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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Security system configuration manager.
 * Loads and provides access to all security-related settings.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class SecurityConfig {

        private static final Logger _log = Logger.getLogger(SecurityConfig.class.getName());
        private static final String CONFIG_FILE = "./config/security.properties";

        // Anti-Dupe Configuration
        public static boolean ANTIDUPE_ENABLED;
        public static int ANTIDUPE_LOCK_TIMEOUT;
        public static boolean ANTIDUPE_LOG_ALL_OPERATIONS;

        // Packet Firewall Configuration
        public static boolean FIREWALL_ENABLED;
        public static int FIREWALL_RATE_GENERAL;
        public static int FIREWALL_RATE_MOVEMENT;
        public static int FIREWALL_RATE_ACTION;
        public static int FIREWALL_RATE_TRADE;
        public static boolean FIREWALL_AUTO_PUNISH;
        public static int FIREWALL_WARN_THRESHOLD;
        public static int FIREWALL_THROTTLE_THRESHOLD;
        public static int FIREWALL_KICK_THRESHOLD;
        public static int FIREWALL_BAN_THRESHOLD;

        // Action Validator Configuration
        public static boolean VALIDATOR_ENABLED;
        public static int VALIDATOR_TOLERANCE_PERCENT;
        public static boolean VALIDATOR_CHECK_ATTACK_SPEED;
        public static boolean VALIDATOR_CHECK_MOVE_SPEED;
        public static boolean VALIDATOR_CHECK_SKILL_REUSE;
        public static boolean VALIDATOR_CHECK_CAST_TIME;

        // Bot Detector Configuration
        public static boolean BOTDETECTOR_ENABLED;
        public static int BOTDETECTOR_SCORE_THRESHOLD_MONITOR;
        public static int BOTDETECTOR_SCORE_THRESHOLD_FLAG;
        public static int BOTDETECTOR_SCORE_THRESHOLD_KICK;
        public static boolean BOTDETECTOR_AUTO_KICK;
        public static int BOTDETECTOR_SCAN_INTERVAL;
        public static int BOTDETECTOR_PATTERN_THRESHOLD;

        // Security Audit Configuration
        public static boolean AUDIT_ENABLED;
        public static boolean AUDIT_LOG_TO_DATABASE;
        public static boolean AUDIT_LOG_TO_FILE;
        public static boolean AUDIT_LOG_GM_COMMANDS;
        public static int AUDIT_RETENTION_DAYS;
        public static boolean AUDIT_LOG_TRADES;
        public static long AUDIT_TRADE_THRESHOLD;
        public static boolean AUDIT_LOG_WAREHOUSE;
        public static long AUDIT_WAREHOUSE_THRESHOLD;

        // Autofarm Integration Configuration
        public static boolean AUTOFARM_ENABLED;
        public static boolean AUTOFARM_EXEMPT_FROM_BOTDETECTOR;
        public static boolean AUTOFARM_EXEMPT_FROM_VALIDATOR;
        public static int AUTOFARM_MIN_LEVEL;
        public static boolean AUTOFARM_ALLOW_IN_OLYMPIAD;
        public static boolean AUTOFARM_ALLOW_IN_EVENTS;

        /**
         * Load security configuration from file.
         */
        public static void load() {
                _log.info("Loading security configuration...");

                final Properties properties = new Properties();
                final File configFile = new File(CONFIG_FILE);

                try (InputStream is = new FileInputStream(configFile)) {
                        properties.load(is);

                        // Anti-Dupe Configuration
                        ANTIDUPE_ENABLED = Boolean.parseBoolean(properties.getProperty("antidupe.enabled", "true"));
                        ANTIDUPE_LOCK_TIMEOUT = Integer
                                        .parseInt(properties.getProperty("antidupe.lock.timeout", "5000"));
                        ANTIDUPE_LOG_ALL_OPERATIONS = Boolean
                                        .parseBoolean(properties.getProperty("antidupe.log.all.operations", "false"));

                        // Packet Firewall Configuration
                        FIREWALL_ENABLED = Boolean.parseBoolean(properties.getProperty("firewall.enabled", "true"));
                        FIREWALL_RATE_GENERAL = Integer
                                        .parseInt(properties.getProperty("firewall.rate.general", "100"));
                        FIREWALL_RATE_MOVEMENT = Integer
                                        .parseInt(properties.getProperty("firewall.rate.movement", "20"));
                        FIREWALL_RATE_ACTION = Integer.parseInt(properties.getProperty("firewall.rate.action", "10"));
                        FIREWALL_RATE_TRADE = Integer.parseInt(properties.getProperty("firewall.rate.trade", "5"));
                        FIREWALL_AUTO_PUNISH = Boolean
                                        .parseBoolean(properties.getProperty("firewall.auto.punish", "true"));
                        FIREWALL_WARN_THRESHOLD = Integer
                                        .parseInt(properties.getProperty("firewall.warn.threshold", "3"));
                        FIREWALL_THROTTLE_THRESHOLD = Integer
                                        .parseInt(properties.getProperty("firewall.throttle.threshold", "5"));
                        FIREWALL_KICK_THRESHOLD = Integer
                                        .parseInt(properties.getProperty("firewall.kick.threshold", "10"));
                        FIREWALL_BAN_THRESHOLD = Integer
                                        .parseInt(properties.getProperty("firewall.ban.threshold", "15"));

                        // Action Validator Configuration
                        VALIDATOR_ENABLED = Boolean.parseBoolean(properties.getProperty("validator.enabled", "true"));
                        VALIDATOR_TOLERANCE_PERCENT = Integer
                                        .parseInt(properties.getProperty("validator.tolerance.percent", "5"));
                        VALIDATOR_CHECK_ATTACK_SPEED = Boolean
                                        .parseBoolean(properties.getProperty("validator.check.attack.speed", "true"));
                        VALIDATOR_CHECK_MOVE_SPEED = Boolean
                                        .parseBoolean(properties.getProperty("validator.check.move.speed", "true"));
                        VALIDATOR_CHECK_SKILL_REUSE = Boolean
                                        .parseBoolean(properties.getProperty("validator.check.skill.reuse", "true"));
                        VALIDATOR_CHECK_CAST_TIME = Boolean
                                        .parseBoolean(properties.getProperty("validator.check.cast.time", "true"));

                        // Bot Detector Configuration
                        BOTDETECTOR_ENABLED = Boolean
                                        .parseBoolean(properties.getProperty("botdetector.enabled", "true"));
                        BOTDETECTOR_SCORE_THRESHOLD_MONITOR = Integer
                                        .parseInt(properties.getProperty("botdetector.score.threshold.monitor", "31"));
                        BOTDETECTOR_SCORE_THRESHOLD_FLAG = Integer
                                        .parseInt(properties.getProperty("botdetector.score.threshold.flag", "61"));
                        BOTDETECTOR_SCORE_THRESHOLD_KICK = Integer
                                        .parseInt(properties.getProperty("botdetector.score.threshold.kick", "81"));
                        BOTDETECTOR_AUTO_KICK = Boolean
                                        .parseBoolean(properties.getProperty("botdetector.auto.kick", "false"));
                        BOTDETECTOR_SCAN_INTERVAL = Integer
                                        .parseInt(properties.getProperty("botdetector.scan.interval", "60000"));
                        BOTDETECTOR_PATTERN_THRESHOLD = Integer
                                        .parseInt(properties.getProperty("botdetector.pattern.threshold", "5"));

                        // Security Audit Configuration
                        AUDIT_ENABLED = Boolean.parseBoolean(properties.getProperty("audit.enabled", "true"));
                        AUDIT_LOG_TO_DATABASE = Boolean
                                        .parseBoolean(properties.getProperty("audit.log.to.database", "true"));
                        AUDIT_LOG_TO_FILE = Boolean.parseBoolean(properties.getProperty("audit.log.to.file", "true"));
                        AUDIT_LOG_GM_COMMANDS = Boolean
                                        .parseBoolean(properties.getProperty("audit.log.gm.commands", "true"));
                        AUDIT_RETENTION_DAYS = Integer.parseInt(properties.getProperty("audit.retention.days", "90"));
                        AUDIT_LOG_TRADES = Boolean.parseBoolean(properties.getProperty("audit.log.trades", "true"));
                        AUDIT_TRADE_THRESHOLD = Long
                                        .parseLong(properties.getProperty("audit.trade.threshold", "1000000"));
                        AUDIT_LOG_WAREHOUSE = Boolean
                                        .parseBoolean(properties.getProperty("audit.log.warehouse", "true"));
                        AUDIT_WAREHOUSE_THRESHOLD = Long
                                        .parseLong(properties.getProperty("audit.warehouse.threshold", "5000000"));

                        // Autofarm Integration Configuration
                        AUTOFARM_ENABLED = Boolean.parseBoolean(properties.getProperty("autofarm.enabled", "true"));
                        AUTOFARM_EXEMPT_FROM_BOTDETECTOR = Boolean
                                        .parseBoolean(properties.getProperty("autofarm.exempt.from.botdetector",
                                                        "true"));
                        AUTOFARM_EXEMPT_FROM_VALIDATOR = Boolean
                                        .parseBoolean(properties.getProperty("autofarm.exempt.from.validator",
                                                        "false"));
                        AUTOFARM_MIN_LEVEL = Integer.parseInt(properties.getProperty("autofarm.min.level", "1"));
                        AUTOFARM_ALLOW_IN_OLYMPIAD = Boolean
                                        .parseBoolean(properties.getProperty("autofarm.allow.in.olympiad", "false"));
                        AUTOFARM_ALLOW_IN_EVENTS = Boolean
                                        .parseBoolean(properties.getProperty("autofarm.allow.in.events", "false"));

                        _log.info("Security configuration loaded successfully.");
                        _log.info("Anti-Dupe: " + (ANTIDUPE_ENABLED ? "ENABLED" : "DISABLED"));
                        _log.info("Packet Firewall: " + (FIREWALL_ENABLED ? "ENABLED" : "DISABLED"));
                        _log.info("Action Validator: " + (VALIDATOR_ENABLED ? "ENABLED" : "DISABLED"));
                        _log.info("Bot Detector: " + (BOTDETECTOR_ENABLED ? "ENABLED" : "DISABLED"));
                        _log.info("Security Audit: " + (AUDIT_ENABLED ? "ENABLED" : "DISABLED"));

                } catch (IOException e) {
                        _log.warning("Could not load security configuration file: " + CONFIG_FILE);
                        _log.warning("Using default security settings.");
                        loadDefaults();
                } catch (NumberFormatException e) {
                        _log.warning("Invalid number format in security configuration: " + e.getMessage());
                        _log.warning("Using default security settings.");
                        loadDefaults();
                }
        }

        /**
         * Load default configuration values.
         */
        private static void loadDefaults() {
                // Anti-Dupe Defaults
                ANTIDUPE_ENABLED = true;
                ANTIDUPE_LOCK_TIMEOUT = 5000;
                ANTIDUPE_LOG_ALL_OPERATIONS = false;

                // Packet Firewall Defaults
                FIREWALL_ENABLED = true;
                FIREWALL_RATE_GENERAL = 100;
                FIREWALL_RATE_MOVEMENT = 20;
                FIREWALL_RATE_ACTION = 10;
                FIREWALL_RATE_TRADE = 5;
                FIREWALL_AUTO_PUNISH = true;
                FIREWALL_WARN_THRESHOLD = 3;
                FIREWALL_THROTTLE_THRESHOLD = 5;
                FIREWALL_KICK_THRESHOLD = 10;
                FIREWALL_BAN_THRESHOLD = 15;

                // Action Validator Defaults
                VALIDATOR_ENABLED = true;
                VALIDATOR_TOLERANCE_PERCENT = 5;
                VALIDATOR_CHECK_ATTACK_SPEED = true;
                VALIDATOR_CHECK_MOVE_SPEED = true;
                VALIDATOR_CHECK_SKILL_REUSE = true;
                VALIDATOR_CHECK_CAST_TIME = true;

                // Bot Detector Defaults
                BOTDETECTOR_ENABLED = true;
                BOTDETECTOR_SCORE_THRESHOLD_MONITOR = 31;
                BOTDETECTOR_SCORE_THRESHOLD_FLAG = 61;
                BOTDETECTOR_SCORE_THRESHOLD_KICK = 81;
                BOTDETECTOR_AUTO_KICK = false;
                BOTDETECTOR_SCAN_INTERVAL = 60000;
                BOTDETECTOR_PATTERN_THRESHOLD = 5;

                // Security Audit Defaults
                AUDIT_ENABLED = true;
                AUDIT_LOG_TO_DATABASE = true;
                AUDIT_LOG_TO_FILE = true;
                AUDIT_LOG_GM_COMMANDS = true;
                AUDIT_RETENTION_DAYS = 90;
                AUDIT_LOG_TRADES = true;
                AUDIT_TRADE_THRESHOLD = 1000000L;
                AUDIT_LOG_WAREHOUSE = true;
                AUDIT_WAREHOUSE_THRESHOLD = 5000000L;

                // Autofarm Integration Defaults
                AUTOFARM_ENABLED = true;
                AUTOFARM_EXEMPT_FROM_BOTDETECTOR = true;
                AUTOFARM_EXEMPT_FROM_VALIDATOR = false;
                AUTOFARM_MIN_LEVEL = 1;
                AUTOFARM_ALLOW_IN_OLYMPIAD = false;
                AUTOFARM_ALLOW_IN_EVENTS = false;
        }

        /**
         * Private constructor to prevent instantiation.
         */
        private SecurityConfig() {
                // Utility class
        }
}
