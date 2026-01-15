-- ================================================================
-- SIGMO Auto Farm System Database Table
-- ================================================================

CREATE TABLE IF NOT EXISTS `character_autofarm` (
    `char_id` INT UNSIGNED NOT NULL,
    `char_name` VARCHAR(50) NOT NULL,
    `radius` INT DEFAULT 1200,
    `short_cut` INT DEFAULT 9,
    `heal_percent` INT DEFAULT 30,
    `buff_protection` BOOLEAN DEFAULT FALSE,
    `anti_ks_protection` BOOLEAN DEFAULT FALSE,
    `summon_attack` BOOLEAN DEFAULT FALSE,
    `summon_skill_percent` INT DEFAULT 0,
    `hp_potion_percent` INT DEFAULT 60,
    `mp_potion_percent` INT DEFAULT 60,
    PRIMARY KEY (`char_id`),
    INDEX `idx_char_name` (`char_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
