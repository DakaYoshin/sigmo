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
package com.gameserver.security.validator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.gameserver.model.L2Skill;
import com.gameserver.model.actor.instance.L2ItemInstance;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.security.core.SecurityConfig;
import com.gameserver.security.core.SecurityLogger;

/**
 * Action validator system.
 * Validates player actions against server-calculated formulas.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class ActionValidator {

    private static final Logger _log = Logger.getLogger(ActionValidator.class.getName());
    private static ActionValidator _instance;

    // Player action tracking: playerId -> PlayerActionTracker
    private final ConcurrentHashMap<Integer, PlayerActionTracker> _trackers;

    /**
     * Player action tracker.
     */
    @SuppressWarnings("unused")
    private static class PlayerActionTracker {
        final int playerId;
        volatile long lastAttackTime;
        volatile long lastMoveTime;
        volatile long lastSkillTime;
        volatile long lastCastTime;
        final AtomicInteger attackSpeedViolations;
        final AtomicInteger moveSpeedViolations;
        final AtomicInteger skillReuseViolations;
        final AtomicInteger castTimeViolations;

        PlayerActionTracker(int playerId) {
            this.playerId = playerId;
            this.lastAttackTime = 0;
            this.lastMoveTime = 0;
            this.lastSkillTime = 0;
            this.lastCastTime = 0;
            this.attackSpeedViolations = new AtomicInteger(0);
            this.moveSpeedViolations = new AtomicInteger(0);
            this.skillReuseViolations = new AtomicInteger(0);
            this.castTimeViolations = new AtomicInteger(0);
        }

        int getTotalViolations() {
            return attackSpeedViolations.get() + moveSpeedViolations.get() +
                    skillReuseViolations.get() + castTimeViolations.get();
        }
    }

    /**
     * Private constructor.
     */
    private ActionValidator() {
        _trackers = new ConcurrentHashMap<>();
        _log.info("ActionValidator initialized.");
    }

    /**
     * Get singleton instance.
     * 
     * @return ActionValidator instance
     */
    public static ActionValidator getInstance() {
        if (_instance == null) {
            synchronized (ActionValidator.class) {
                if (_instance == null) {
                    _instance = new ActionValidator();
                }
            }
        }
        return _instance;
    }

    /**
     * Validate attack action.
     * 
     * @param player Player performing attack
     * @return true if attack is valid
     */
    public boolean validateAttack(L2PcInstance player) {
        if (!SecurityConfig.VALIDATOR_ENABLED || !SecurityConfig.VALIDATOR_CHECK_ATTACK_SPEED) {
            return true;
        }

        if (player == null) {
            return false;
        }

        final PlayerActionTracker tracker = _trackers.computeIfAbsent(
                player.getObjectId(), PlayerActionTracker::new);

        final long now = System.currentTimeMillis();

        if (tracker.lastAttackTime > 0) {
            // Calculate expected attack delay based on player's attack speed
            final int expectedDelay = calculateAttackDelay(player);
            final long actualDelay = now - tracker.lastAttackTime;

            // Allow tolerance for network latency
            final int minDelay = expectedDelay - (expectedDelay * SecurityConfig.VALIDATOR_TOLERANCE_PERCENT / 100);

            if (actualDelay < minDelay) {
                handleViolation(player, tracker, "ATTACK_SPEED",
                        String.format("Expected=%dms Actual=%dms", expectedDelay, actualDelay));
                tracker.attackSpeedViolations.incrementAndGet();
                return false;
            }
        }

        tracker.lastAttackTime = now;
        return true;
    }

    /**
     * Validate movement action.
     * 
     * @param player    Player moving
     * @param distance  Distance moved
     * @param timeDelta Time elapsed
     * @return true if movement is valid
     */
    public boolean validateMovement(L2PcInstance player, double distance, long timeDelta) {
        if (!SecurityConfig.VALIDATOR_ENABLED || !SecurityConfig.VALIDATOR_CHECK_MOVE_SPEED) {
            return true;
        }

        if (player == null || timeDelta <= 0) {
            return false;
        }

        final PlayerActionTracker tracker = _trackers.computeIfAbsent(
                player.getObjectId(), PlayerActionTracker::new);

        // Calculate expected max distance based on player's move speed
        final double maxSpeed = calculateMoveSpeed(player);
        final double maxDistance = (maxSpeed * timeDelta) / 1000.0; // Convert to seconds

        // Allow tolerance
        final double maxAllowedDistance = maxDistance * (1.0 + SecurityConfig.VALIDATOR_TOLERANCE_PERCENT / 100.0);

        if (distance > maxAllowedDistance) {
            handleViolation(player, tracker, "MOVE_SPEED",
                    String.format("Distance=%.2f MaxAllowed=%.2f Time=%dms",
                            distance, maxAllowedDistance, timeDelta));
            tracker.moveSpeedViolations.incrementAndGet();
            return false;
        }

        tracker.lastMoveTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Validate skill usage.
     * 
     * @param player Player using skill
     * @param skill  Skill being used
     * @return true if skill usage is valid
     */
    public boolean validateSkillUse(L2PcInstance player, L2Skill skill) {
        if (!SecurityConfig.VALIDATOR_ENABLED || !SecurityConfig.VALIDATOR_CHECK_SKILL_REUSE) {
            return true;
        }

        if (player == null || skill == null) {
            return false;
        }

        final PlayerActionTracker tracker = _trackers.computeIfAbsent(
                player.getObjectId(), PlayerActionTracker::new);

        // Check skill reuse time
        final L2PcInstance.TimeStamp timeStamp = player.getReuseTimeStamp().get(skill.getId());

        if (timeStamp != null && timeStamp.hasNotPassed()) {
            final long remaining = timeStamp.getRemaining();
            handleViolation(player, tracker, "SKILL_REUSE",
                    String.format("Skill=%s[%d] RemainingCooldown=%dms",
                            skill.getName(), skill.getId(), remaining));
            tracker.skillReuseViolations.incrementAndGet();
            return false;
        }

        tracker.lastSkillTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Validate skill cast time.
     * 
     * @param player   Player casting skill
     * @param skill    Skill being cast
     * @param castTime Reported cast time
     * @return true if cast time is valid
     */
    public boolean validateCastTime(L2PcInstance player, L2Skill skill, int castTime) {
        if (!SecurityConfig.VALIDATOR_ENABLED || !SecurityConfig.VALIDATOR_CHECK_CAST_TIME) {
            return true;
        }

        if (player == null || skill == null) {
            return false;
        }

        final PlayerActionTracker tracker = _trackers.computeIfAbsent(
                player.getObjectId(), PlayerActionTracker::new);

        // Calculate expected cast time
        final int expectedCastTime = calculateCastTime(player, skill);

        // Allow tolerance
        final int minCastTime = expectedCastTime
                - (expectedCastTime * SecurityConfig.VALIDATOR_TOLERANCE_PERCENT / 100);

        if (castTime < minCastTime) {
            handleViolation(player, tracker, "CAST_TIME",
                    String.format("Skill=%s[%d] Expected=%dms Actual=%dms",
                            skill.getName(), skill.getId(), expectedCastTime, castTime));
            tracker.castTimeViolations.incrementAndGet();
            return false;
        }

        tracker.lastCastTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Calculate expected attack delay.
     * 
     * @param player Player
     * @return Attack delay in milliseconds
     */
    private int calculateAttackDelay(L2PcInstance player) {
        // Base attack speed calculation
        // This is simplified - adjust based on your server's formulas

        int pAtkSpd = player.getPAtkSpd();

        // Get weapon attack speed
        final L2ItemInstance weapon = player.getActiveWeaponInstance();
        int weaponAtkSpd = 0;

        if (weapon != null && weapon.getItem() instanceof com.gameserver.templates.item.L2Weapon) {
            weaponAtkSpd = ((com.gameserver.templates.item.L2Weapon) weapon.getItem()).getAttackSpeed();
        }

        if (weaponAtkSpd == 0) {
            weaponAtkSpd = 333; // Default fist attack speed
        }

        // Calculate delay: baseSpeed * 1000 / pAtkSpd
        return (int) ((weaponAtkSpd * 1000.0) / pAtkSpd);
    }

    /**
     * Calculate expected move speed.
     * 
     * @param player Player
     * @return Move speed
     */
    private double calculateMoveSpeed(L2PcInstance player) {
        // Get base move speed from player stats
        double speed = player.getRunSpeed();

        // Apply any active speed buffs/debuffs
        // This is handled by the player's stat calculation

        return speed;
    }

    /**
     * Calculate expected cast time.
     * 
     * @param player Player
     * @param skill  Skill
     * @return Cast time in milliseconds
     */
    private int calculateCastTime(L2PcInstance player, L2Skill skill) {
        // Get base cast time from skill
        int baseCastTime = skill.getHitTime();

        // Apply magic speed modifier
        int mAtkSpd = player.getMAtkSpd();

        // Calculate modified cast time
        // Formula: baseCastTime * 333 / mAtkSpd
        return (int) ((baseCastTime * 333.0) / mAtkSpd);
    }

    /**
     * Handle validation violation.
     * 
     * @param player        Player
     * @param tracker       Action tracker
     * @param violationType Type of violation
     * @param details       Violation details
     */
    private void handleViolation(L2PcInstance player, PlayerActionTracker tracker,
            String violationType, String details) {
        SecurityLogger.getInstance().logf("VALIDATOR",
                "VIOLATION: Player=%s[%d] Type=%s Details=%s TotalViolations=%d",
                player.getName(), player.getObjectId(), violationType, details,
                tracker.getTotalViolations());

        // Progressive punishment
        final int totalViolations = tracker.getTotalViolations();

        if (totalViolations >= 20) {
            _log.warning(String.format("Player %s has excessive action violations (%d) - consider investigation",
                    player.getName(), totalViolations));
            player.sendMessage("Warning: Suspicious activity detected. You are being monitored.");
        } else if (totalViolations >= 10) {
            player.sendMessage("Warning: Action speed violation detected.");
        }
    }

    /**
     * Remove player tracker (on disconnect).
     * 
     * @param player Player
     */
    public void removePlayer(L2PcInstance player) {
        if (player != null) {
            _trackers.remove(player.getObjectId());
        }
    }

    /**
     * Get player violations.
     * 
     * @param player Player
     * @return Total violations
     */
    public int getPlayerViolations(L2PcInstance player) {
        if (player == null) {
            return 0;
        }

        final PlayerActionTracker tracker = _trackers.get(player.getObjectId());
        return tracker != null ? tracker.getTotalViolations() : 0;
    }

    /**
     * Get statistics.
     * 
     * @return Statistics string
     */
    public String getStats() {
        int totalViolations = 0;
        for (PlayerActionTracker tracker : _trackers.values()) {
            totalViolations += tracker.getTotalViolations();
        }

        return String.format("ActionValidator Stats - Tracked Players: %d, Total Violations: %d",
                _trackers.size(), totalViolations);
    }
}
