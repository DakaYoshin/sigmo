-- ================================================================
-- SIGMO Security System Database Tables
-- ================================================================
-- Version: 1.0.0
-- Database: MariaDB 10+
-- ================================================================

-- ----------------------------------------------------------------
-- Security Audit Log Table
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `security_audit` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `event_type` VARCHAR(50) NOT NULL,
    `player_id` INT UNSIGNED DEFAULT NULL,
    `player_name` VARCHAR(50) DEFAULT NULL,
    `account_name` VARCHAR(50) DEFAULT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `details` TEXT DEFAULT NULL,
    `severity` ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL DEFAULT 'INFO',
    PRIMARY KEY (`id`),
    INDEX `idx_timestamp` (`timestamp`),
    INDEX `idx_player_id` (`player_id`),
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_severity` (`severity`),
    INDEX `idx_account` (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Security audit log for all critical events';

-- ----------------------------------------------------------------
-- Security Violations Table
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `security_violations` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `player_id` INT UNSIGNED NOT NULL,
    `player_name` VARCHAR(50) NOT NULL,
    `account_name` VARCHAR(50) NOT NULL,
    `violation_type` VARCHAR(50) NOT NULL,
    `violation_count` INT UNSIGNED DEFAULT 1,
    `last_violation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `punishment_level` TINYINT UNSIGNED DEFAULT 0,
    `is_banned` BOOLEAN DEFAULT FALSE,
    `ban_expiry` TIMESTAMP NULL DEFAULT NULL,
    `details` TEXT DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_player_id` (`player_id`),
    INDEX `idx_violation_type` (`violation_type`),
    INDEX `idx_timestamp` (`timestamp`),
    INDEX `idx_banned` (`is_banned`),
    INDEX `idx_account` (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Security violation tracking and punishment history';

-- ----------------------------------------------------------------
-- Bot Detection Scores Table
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bot_detection_scores` (
    `player_id` INT UNSIGNED NOT NULL,
    `player_name` VARCHAR(50) NOT NULL,
    `account_name` VARCHAR(50) NOT NULL,
    `score` INT UNSIGNED DEFAULT 0,
    `last_updated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `pattern_violations` INT UNSIGNED DEFAULT 0,
    `timing_violations` INT UNSIGNED DEFAULT 0,
    `path_violations` INT UNSIGNED DEFAULT 0,
    `flagged_for_review` BOOLEAN DEFAULT FALSE,
    `review_notes` TEXT DEFAULT NULL,
    `reviewed_by` VARCHAR(50) DEFAULT NULL,
    `review_date` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`player_id`),
    INDEX `idx_score` (`score`),
    INDEX `idx_flagged` (`flagged_for_review`),
    INDEX `idx_last_updated` (`last_updated`),
    INDEX `idx_account` (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bot detection scores and analysis data';

-- ----------------------------------------------------------------
-- Item Transaction Log Table (for anti-dupe tracking)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `item_transaction_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `transaction_id` BIGINT UNSIGNED NOT NULL,
    `player_id` INT UNSIGNED NOT NULL,
    `player_name` VARCHAR(50) NOT NULL,
    `item_object_id` INT UNSIGNED NOT NULL,
    `item_id` INT UNSIGNED NOT NULL,
    `item_count` BIGINT NOT NULL,
    `operation_type` VARCHAR(50) NOT NULL,
    `success` BOOLEAN DEFAULT TRUE,
    `details` TEXT DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_timestamp` (`timestamp`),
    INDEX `idx_transaction_id` (`transaction_id`),
    INDEX `idx_player_id` (`player_id`),
    INDEX `idx_item_object_id` (`item_object_id`),
    INDEX `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Item transaction log for anti-duplication tracking';

-- ----------------------------------------------------------------
-- Packet Firewall Statistics Table
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `packet_firewall_stats` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `player_id` INT UNSIGNED NOT NULL,
    `player_name` VARCHAR(50) NOT NULL,
    `account_name` VARCHAR(50) NOT NULL,
    `ip_address` VARCHAR(45) NOT NULL,
    `total_packets` BIGINT UNSIGNED DEFAULT 0,
    `rejected_packets` INT UNSIGNED DEFAULT 0,
    `violation_type` VARCHAR(50) DEFAULT NULL,
    `punishment_applied` VARCHAR(50) DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_timestamp` (`timestamp`),
    INDEX `idx_player_id` (`player_id`),
    INDEX `idx_ip_address` (`ip_address`),
    INDEX `idx_account` (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Packet firewall statistics and violations';

-- ================================================================
-- Cleanup Procedures
-- ================================================================

-- ----------------------------------------------------------------
-- Procedure to cleanup old audit logs
-- ----------------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS `sp_cleanup_security_audit`()
BEGIN
    DECLARE retention_days INT DEFAULT 90;
    
    -- Get retention setting (you can modify this to read from a config table)
    -- SET retention_days = (SELECT value FROM config WHERE key = 'audit.retention.days');
    
    -- Delete old audit logs
    DELETE FROM `security_audit`
    WHERE `timestamp` < DATE_SUB(NOW(), INTERVAL retention_days DAY);
    
    -- Log cleanup action
    INSERT INTO `security_audit` 
        (`event_type`, `details`, `severity`)
    VALUES 
        ('SYSTEM', CONCAT('Cleaned up audit logs older than ', retention_days, ' days'), 'INFO');
    
    SELECT CONCAT('Cleanup complete. Deleted records older than ', retention_days, ' days') AS result;
END$$

DELIMITER ;

-- ----------------------------------------------------------------
-- Procedure to get player security summary
-- ----------------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS `sp_get_player_security_summary`(IN p_player_id INT)
BEGIN
    SELECT 
        'Violations' AS category,
        COUNT(*) AS count,
        MAX(last_violation) AS last_occurrence
    FROM security_violations
    WHERE player_id = p_player_id
    
    UNION ALL
    
    SELECT 
        'Bot Score' AS category,
        score AS count,
        last_updated AS last_occurrence
    FROM bot_detection_scores
    WHERE player_id = p_player_id
    
    UNION ALL
    
    SELECT 
        'Audit Events' AS category,
        COUNT(*) AS count,
        MAX(timestamp) AS last_occurrence
    FROM security_audit
    WHERE player_id = p_player_id;
END$$

DELIMITER ;

-- ================================================================
-- Initial Data / Configuration
-- ================================================================

-- Log installation
INSERT INTO `security_audit` 
    (`event_type`, `details`, `severity`)
VALUES 
    ('SYSTEM', 'Security system database tables installed successfully', 'INFO');

-- ================================================================
-- Indexes Optimization (Optional - for large databases)
-- ================================================================

-- Uncomment if you have a very large database and need additional optimization
-- ALTER TABLE `security_audit` ADD INDEX `idx_composite_player_time` (`player_id`, `timestamp`);
-- ALTER TABLE `security_violations` ADD INDEX `idx_composite_player_type` (`player_id`, `violation_type`);

-- ================================================================
-- End of Installation Script
-- ================================================================

SELECT '==================================================' AS '';
SELECT 'SIGMO Security System Database Installation' AS '';
SELECT 'Version 1.0.0' AS '';
SELECT '==================================================' AS '';
SELECT 'Tables created:' AS '';
SELECT '  - security_audit' AS '';
SELECT '  - security_violations' AS '';
SELECT '  - bot_detection_scores' AS '';
SELECT '  - item_transaction_log' AS '';
SELECT '  - packet_firewall_stats' AS '';
SELECT '' AS '';
SELECT 'Procedures created:' AS '';
SELECT '  - sp_cleanup_security_audit' AS '';
SELECT '  - sp_get_player_security_summary' AS '';
SELECT '' AS '';
SELECT 'Installation complete!' AS '';
SELECT '==================================================' AS '';
