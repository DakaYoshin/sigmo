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
package com.gameserver.security.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.L2World;
import com.gameserver.security.core.SecurityConfig;
import com.gameserver.security.core.SecurityLogger;
import com.gameserver.thread.ThreadPoolManager;
import com.util.object.Point3D;

/**
 * Bot detection system.
 * Uses heuristic analysis to detect automated gameplay patterns.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class BotDetector implements Runnable {

    private static final Logger _log = Logger.getLogger(BotDetector.class.getName());
    private static BotDetector _instance;

    // Player behavior tracking: playerId -> BehaviorTracker
    private final ConcurrentHashMap<Integer, BehaviorTracker> _trackers;

    // Path history size
    private static final int PATH_HISTORY_SIZE = 20;

    // Timing history size
    private static final int TIMING_HISTORY_SIZE = 50;

    /**
     * Behavior tracker for a player.
     */
    private static class BehaviorTracker {
        final int playerId;
        final AtomicInteger botScore;
        final List<Point3D> pathHistory;
        final List<Long> actionTimings;
        volatile long loginTime;
        volatile long lastActionTime;
        volatile boolean flaggedForReview;

        BehaviorTracker(int playerId) {
            this.playerId = playerId;
            this.botScore = new AtomicInteger(0);
            this.pathHistory = new ArrayList<>(PATH_HISTORY_SIZE);
            this.actionTimings = new ArrayList<>(TIMING_HISTORY_SIZE);
            this.loginTime = System.currentTimeMillis();
            this.lastActionTime = 0;
            this.flaggedForReview = false;
        }
    }

    /**
     * Private constructor.
     */
    private BotDetector() {
        _trackers = new ConcurrentHashMap<>();

        // Start detection task
        if (SecurityConfig.BOTDETECTOR_ENABLED) {
            ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                    this,
                    SecurityConfig.BOTDETECTOR_SCAN_INTERVAL,
                    SecurityConfig.BOTDETECTOR_SCAN_INTERVAL);
        }

        _log.info("BotDetector initialized.");
    }

    /**
     * Get singleton instance.
     * 
     * @return BotDetector instance
     */
    public static BotDetector getInstance() {
        if (_instance == null) {
            synchronized (BotDetector.class) {
                if (_instance == null) {
                    _instance = new BotDetector();
                }
            }
        }
        return _instance;
    }

    /**
     * Track player action.
     * 
     * @param player Player
     */
    public void trackAction(L2PcInstance player) {
        if (!SecurityConfig.BOTDETECTOR_ENABLED || player == null) {
            return;
        }

        // Skip tracking for players using legitimate autofarm
        if (SecurityConfig.AUTOFARM_EXEMPT_FROM_BOTDETECTOR && player.isAutoFarm()) {
            return;
        }

        final BehaviorTracker tracker = _trackers.computeIfAbsent(
                player.getObjectId(), BehaviorTracker::new);

        final long now = System.currentTimeMillis();

        // Track action timing
        if (tracker.lastActionTime > 0) {
            final long interval = now - tracker.lastActionTime;

            synchronized (tracker.actionTimings) {
                tracker.actionTimings.add(interval);

                // Keep only recent timings
                if (tracker.actionTimings.size() > TIMING_HISTORY_SIZE) {
                    tracker.actionTimings.remove(0);
                }
            }
        }

        tracker.lastActionTime = now;
    }

    /**
     * Track player movement.
     * 
     * @param player Player
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param z      Z coordinate
     */
    public void trackMovement(L2PcInstance player, int x, int y, int z) {
        if (!SecurityConfig.BOTDETECTOR_ENABLED || player == null) {
            return;
        }

        // Skip tracking for players using legitimate autofarm
        if (SecurityConfig.AUTOFARM_EXEMPT_FROM_BOTDETECTOR && player.isAutoFarm()) {
            return;
        }

        final BehaviorTracker tracker = _trackers.computeIfAbsent(
                player.getObjectId(), BehaviorTracker::new);

        final Point3D point = new Point3D(x, y, z);

        synchronized (tracker.pathHistory) {
            tracker.pathHistory.add(point);

            // Keep only recent path
            if (tracker.pathHistory.size() > PATH_HISTORY_SIZE) {
                tracker.pathHistory.remove(0);
            }
        }
    }

    /**
     * Periodic detection scan.
     */
    @Override
    public void run() {
        try {
            for (BehaviorTracker tracker : _trackers.values()) {
                analyzePlayerBehavior(tracker);
            }
        } catch (Exception e) {
            _log.warning("Error in BotDetector scan: " + e.getMessage());
        }
    }

    /**
     * Analyze player behavior and update bot score.
     * 
     * @param tracker Behavior tracker
     */
    private void analyzePlayerBehavior(BehaviorTracker tracker) {
        int score = 0;

        // Check for fixed timing patterns
        score += analyzeTimingPatterns(tracker);

        // Check for path repetition
        score += analyzePathPatterns(tracker);

        // Check for excessive play time
        score += analyzePlayTime(tracker);

        // Update bot score
        final int oldScore = tracker.botScore.getAndSet(score);

        // Check thresholds
        if (score >= SecurityConfig.BOTDETECTOR_SCORE_THRESHOLD_KICK &&
                oldScore < SecurityConfig.BOTDETECTOR_SCORE_THRESHOLD_KICK) {

            SecurityLogger.getInstance().logf("BOTDETECTOR",
                    "HIGH_SCORE: PlayerId=%d Score=%d (KICK threshold reached)",
                    tracker.playerId, score);

            if (SecurityConfig.BOTDETECTOR_AUTO_KICK) {
                // Kick the suspected bot
                final L2PcInstance player = L2World.getInstance().getPlayer(tracker.playerId);
                if (player != null) {
                    _log.warning(String.format("Auto-kicking suspected bot: %s[%d] Score=%d",
                            player.getName(), tracker.playerId, score));
                    player.sendMessage("You have been disconnected due to suspicious bot-like behavior.");
                    player.logout();
                } else {
                    _log.warning(String.format("Cannot kick suspected bot: PlayerId=%d (player not found)",
                            tracker.playerId));
                }
            }

        } else if (score >= SecurityConfig.BOTDETECTOR_SCORE_THRESHOLD_FLAG &&
                !tracker.flaggedForReview) {

            tracker.flaggedForReview = true;
            SecurityLogger.getInstance().logf("BOTDETECTOR",
                    "FLAGGED: PlayerId=%d Score=%d (flagged for GM review)",
                    tracker.playerId, score);

        } else if (score >= SecurityConfig.BOTDETECTOR_SCORE_THRESHOLD_MONITOR &&
                oldScore < SecurityConfig.BOTDETECTOR_SCORE_THRESHOLD_MONITOR) {

            SecurityLogger.getInstance().logf("BOTDETECTOR",
                    "SUSPICIOUS: PlayerId=%d Score=%d (monitoring)",
                    tracker.playerId, score);
        }
    }

    /**
     * Analyze timing patterns.
     * 
     * @param tracker Behavior tracker
     * @return Score contribution
     */
    private int analyzeTimingPatterns(BehaviorTracker tracker) {
        synchronized (tracker.actionTimings) {
            if (tracker.actionTimings.size() < 10) {
                return 0;
            }

            // Check for fixed intervals (±10ms)
            int fixedIntervals = 0;
            long lastInterval = 0;

            for (long interval : tracker.actionTimings) {
                if (lastInterval > 0) {
                    final long diff = Math.abs(interval - lastInterval);
                    if (diff <= 10) {
                        fixedIntervals++;
                    }
                }
                lastInterval = interval;
            }

            // If more than 70% of intervals are fixed, likely a bot
            final double fixedRatio = (double) fixedIntervals / tracker.actionTimings.size();

            if (fixedRatio > 0.7) {
                return 30; // High score for fixed timing
            } else if (fixedRatio > 0.5) {
                return 15; // Medium score
            }
        }

        return 0;
    }

    /**
     * Analyze path patterns.
     * 
     * @param tracker Behavior tracker
     * @return Score contribution
     */
    private int analyzePathPatterns(BehaviorTracker tracker) {
        synchronized (tracker.pathHistory) {
            if (tracker.pathHistory.size() < PATH_HISTORY_SIZE) {
                return 0;
            }

            // Check for repeating path sequences
            final int sequenceLength = 5;
            int repetitions = 0;

            for (int i = 0; i <= tracker.pathHistory.size() - sequenceLength * 2; i++) {
                final List<Point3D> sequence1 = tracker.pathHistory.subList(i, i + sequenceLength);

                for (int j = i + sequenceLength; j <= tracker.pathHistory.size() - sequenceLength; j++) {
                    final List<Point3D> sequence2 = tracker.pathHistory.subList(j, j + sequenceLength);

                    if (pathSequencesMatch(sequence1, sequence2)) {
                        repetitions++;
                        break;
                    }
                }
            }

            if (repetitions >= SecurityConfig.BOTDETECTOR_PATTERN_THRESHOLD) {
                return 25; // High score for path repetition
            } else if (repetitions >= SecurityConfig.BOTDETECTOR_PATTERN_THRESHOLD / 2) {
                return 10; // Medium score
            }
        }

        return 0;
    }

    /**
     * Check if two path sequences match.
     * 
     * @param seq1 First sequence
     * @param seq2 Second sequence
     * @return true if sequences match
     */
    private boolean pathSequencesMatch(List<Point3D> seq1, List<Point3D> seq2) {
        if (seq1.size() != seq2.size()) {
            return false;
        }

        final int tolerance = 100; // 100 units tolerance

        for (int i = 0; i < seq1.size(); i++) {
            final Point3D p1 = seq1.get(i);
            final Point3D p2 = seq2.get(i);

            final double distance = Math.sqrt(
                    Math.pow(p1.getX() - p2.getX(), 2) +
                            Math.pow(p1.getY() - p2.getY(), 2) +
                            Math.pow(p1.getZ() - p2.getZ(), 2));

            if (distance > tolerance) {
                return false;
            }
        }

        return true;
    }

    /**
     * Analyze play time.
     * 
     * @param tracker Behavior tracker
     * @return Score contribution
     */
    private int analyzePlayTime(BehaviorTracker tracker) {
        final long now = System.currentTimeMillis();
        final long sessionTime = now - tracker.loginTime;

        // Check for excessive continuous play (>20 hours)
        if (sessionTime > 72000000) { // 20 hours
            return 20;
        } else if (sessionTime > 54000000) { // 15 hours
            return 10;
        }

        return 0;
    }

    /**
     * Get player bot score.
     * 
     * @param player Player
     * @return Bot score (0-100)
     */
    public int getPlayerScore(L2PcInstance player) {
        if (player == null) {
            return 0;
        }

        final BehaviorTracker tracker = _trackers.get(player.getObjectId());
        return tracker != null ? tracker.botScore.get() : 0;
    }

    /**
     * Check if player is flagged for review.
     * 
     * @param player Player
     * @return true if flagged
     */
    public boolean isPlayerFlagged(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        final BehaviorTracker tracker = _trackers.get(player.getObjectId());
        return tracker != null && tracker.flaggedForReview;
    }

    /**
     * Clear player flag.
     * 
     * @param player Player
     */
    public void clearPlayerFlag(L2PcInstance player) {
        if (player == null) {
            return;
        }

        final BehaviorTracker tracker = _trackers.get(player.getObjectId());
        if (tracker != null) {
            tracker.flaggedForReview = false;
            tracker.botScore.set(0);
            SecurityLogger.getInstance().logf("BOTDETECTOR",
                    "FLAG_CLEARED: PlayerId=%d", tracker.playerId);
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
     * Get statistics.
     * 
     * @return Statistics string
     */
    public String getStats() {
        int flaggedCount = 0;
        int highScoreCount = 0;

        for (BehaviorTracker tracker : _trackers.values()) {
            if (tracker.flaggedForReview) {
                flaggedCount++;
            }
            if (tracker.botScore.get() >= SecurityConfig.BOTDETECTOR_SCORE_THRESHOLD_KICK) {
                highScoreCount++;
            }
        }

        return String.format("BotDetector Stats - Tracked: %d, Flagged: %d, High Score: %d",
                _trackers.size(), flaggedCount, highScoreCount);
    }
}
