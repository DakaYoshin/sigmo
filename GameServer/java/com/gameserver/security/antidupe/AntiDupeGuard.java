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
package com.gameserver.security.antidupe;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import com.gameserver.model.actor.instance.L2ItemInstance;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.security.core.SecurityConfig;
import com.gameserver.security.core.SecurityLogger;

/**
 * Anti-duplication guard system.
 * Prevents item duplication exploits through transaction locking and
 * validation.
 * 
 * @author SIGMO Security Team
 * @version 1.0.0
 */
public final class AntiDupeGuard {

    private static final Logger _log = Logger.getLogger(AntiDupeGuard.class.getName());
    private static AntiDupeGuard _instance;

    // Transaction lock map: lockKey -> TransactionLock
    private final ConcurrentHashMap<String, TransactionLock> _locks;

    // Transaction sequence counter
    private final AtomicLong _transactionSequence;

    // Active transaction map: charId -> TransactionInfo
    private final ConcurrentHashMap<Integer, TransactionInfo> _activeTransactions;

    /**
     * Transaction lock wrapper.
     */
    private static class TransactionLock {
        final ReentrantLock lock;
        final long createdTime;
        volatile long lastAccessTime;

        TransactionLock() {
            this.lock = new ReentrantLock(true); // Fair lock
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = createdTime;
        }
    }

    /**
     * Transaction information.
     */
    @SuppressWarnings("unused")
    private static class TransactionInfo {
        final long transactionId;
        final String operationType;
        final int itemObjectId;
        final long startTime;

        TransactionInfo(long transactionId, String operationType, int itemObjectId) {
            this.transactionId = transactionId;
            this.operationType = operationType;
            this.itemObjectId = itemObjectId;
            this.startTime = System.currentTimeMillis();
        }
    }

    /**
     * Private constructor.
     */
    private AntiDupeGuard() {
        _locks = new ConcurrentHashMap<>();
        _transactionSequence = new AtomicLong(0);
        _activeTransactions = new ConcurrentHashMap<>();

        // Start cleanup task
        startCleanupTask();

        _log.info("AntiDupeGuard initialized.");
    }

    /**
     * Get singleton instance.
     * 
     * @return AntiDupeGuard instance
     */
    public static AntiDupeGuard getInstance() {
        if (_instance == null) {
            synchronized (AntiDupeGuard.class) {
                if (_instance == null) {
                    _instance = new AntiDupeGuard();
                }
            }
        }
        return _instance;
    }

    /**
     * Acquire item lock for operation.
     * 
     * @param player        Player performing operation
     * @param item          Item being operated on
     * @param operationType Type of operation (TRADE, WAREHOUSE, DROP, etc.)
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireItemLock(L2PcInstance player, L2ItemInstance item, String operationType) {
        if (!SecurityConfig.ANTIDUPE_ENABLED) {
            return true;
        }

        if (player == null || item == null) {
            return false;
        }

        final String lockKey = generateLockKey(player.getObjectId(), item.getObjectId(), operationType);
        final TransactionLock transactionLock = _locks.computeIfAbsent(lockKey, k -> new TransactionLock());

        try {
            // Try to acquire lock with timeout
            if (transactionLock.lock.tryLock(SecurityConfig.ANTIDUPE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                transactionLock.lastAccessTime = System.currentTimeMillis();

                // Check if player already has an active transaction
                final TransactionInfo existingTx = _activeTransactions.get(player.getObjectId());
                if (existingTx != null && existingTx.itemObjectId == item.getObjectId()) {
                    _log.warning(String.format("Player %s attempted duplicate transaction on item %d",
                            player.getName(), item.getObjectId()));
                    SecurityLogger.getInstance().logf("ANTIDUPE",
                            "DUPLICATE_TRANSACTION: Player=%s[%d] Item=%d Operation=%s",
                            player.getName(), player.getObjectId(), item.getObjectId(), operationType);
                    transactionLock.lock.unlock();
                    return false;
                }

                // Create transaction info
                final long txId = _transactionSequence.incrementAndGet();
                final TransactionInfo txInfo = new TransactionInfo(txId, operationType, item.getObjectId());
                _activeTransactions.put(player.getObjectId(), txInfo);

                if (SecurityConfig.ANTIDUPE_LOG_ALL_OPERATIONS) {
                    SecurityLogger.getInstance().logf("ANTIDUPE",
                            "LOCK_ACQUIRED: TxId=%d Player=%s[%d] Item=%d[%s] Operation=%s",
                            txId, player.getName(), player.getObjectId(),
                            item.getObjectId(), item.getName(), operationType);
                }

                return true;
            } else {
                _log.warning(String.format("Failed to acquire lock for player %s, item %d (timeout)",
                        player.getName(), item.getObjectId()));
                SecurityLogger.getInstance().logf("ANTIDUPE",
                        "LOCK_TIMEOUT: Player=%s[%d] Item=%d Operation=%s",
                        player.getName(), player.getObjectId(), item.getObjectId(), operationType);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            _log.warning("Lock acquisition interrupted for player " + player.getName());
            return false;
        }
    }

    /**
     * Release item lock.
     * 
     * @param player        Player performing operation
     * @param item          Item being operated on
     * @param operationType Type of operation
     */
    public void releaseItemLock(L2PcInstance player, L2ItemInstance item, String operationType) {
        if (!SecurityConfig.ANTIDUPE_ENABLED) {
            return;
        }

        if (player == null || item == null) {
            return;
        }

        final String lockKey = generateLockKey(player.getObjectId(), item.getObjectId(), operationType);
        final TransactionLock transactionLock = _locks.get(lockKey);

        if (transactionLock != null && transactionLock.lock.isHeldByCurrentThread()) {
            // Remove transaction info
            final TransactionInfo txInfo = _activeTransactions.remove(player.getObjectId());

            if (SecurityConfig.ANTIDUPE_LOG_ALL_OPERATIONS && txInfo != null) {
                final long duration = System.currentTimeMillis() - txInfo.startTime;
                SecurityLogger.getInstance().logf("ANTIDUPE",
                        "LOCK_RELEASED: TxId=%d Player=%s[%d] Item=%d Duration=%dms",
                        txInfo.transactionId, player.getName(), player.getObjectId(),
                        item.getObjectId(), duration);
            }

            transactionLock.lock.unlock();
        }
    }

    /**
     * Validate item operation.
     * Checks if the operation is valid and not a duplicate.
     * 
     * @param player        Player performing operation
     * @param item          Item being operated on
     * @param operationType Type of operation
     * @return true if operation is valid
     */
    public boolean validateItemOperation(L2PcInstance player, L2ItemInstance item, String operationType) {
        if (!SecurityConfig.ANTIDUPE_ENABLED) {
            return true;
        }

        if (player == null || item == null) {
            return false;
        }

        // Check if item still exists in player's inventory
        if (item.getOwnerId() != player.getObjectId()) {
            _log.warning(String.format("Player %s attempted operation on item %d not owned by them",
                    player.getName(), item.getObjectId()));
            SecurityLogger.getInstance().logf("ANTIDUPE",
                    "INVALID_OWNER: Player=%s[%d] Item=%d ActualOwner=%d Operation=%s",
                    player.getName(), player.getObjectId(), item.getObjectId(),
                    item.getOwnerId(), operationType);
            return false;
        }

        // Check if player has active transaction on this item
        final TransactionInfo txInfo = _activeTransactions.get(player.getObjectId());
        if (txInfo != null && txInfo.itemObjectId != item.getObjectId()) {
            _log.warning(String.format("Player %s has active transaction on different item",
                    player.getName()));
            SecurityLogger.getInstance().logf("ANTIDUPE",
                    "CONCURRENT_TRANSACTION: Player=%s[%d] ActiveItem=%d RequestedItem=%d",
                    player.getName(), player.getObjectId(), txInfo.itemObjectId, item.getObjectId());
            return false;
        }

        return true;
    }

    /**
     * Force release all locks for a player.
     * Used when player disconnects or dies.
     * 
     * @param player Player
     */
    public void releaseAllLocks(L2PcInstance player) {
        if (player == null) {
            return;
        }

        final TransactionInfo txInfo = _activeTransactions.remove(player.getObjectId());

        if (txInfo != null) {
            _log.info(String.format("Force releasing locks for player %s (TxId=%d)",
                    player.getName(), txInfo.transactionId));

            // Find and release all locks held by this player
            _locks.forEach((key, lock) -> {
                if (key.startsWith(player.getObjectId() + "_") && lock.lock.isHeldByCurrentThread()) {
                    lock.lock.unlock();
                }
            });
        }
    }

    /**
     * Generate lock key.
     * 
     * @param charId        Character ID
     * @param itemId        Item object ID
     * @param operationType Operation type
     * @return Lock key
     */
    private String generateLockKey(int charId, int itemId, String operationType) {
        return charId + "_" + itemId + "_" + operationType;
    }

    /**
     * Start cleanup task for expired locks.
     */
    private void startCleanupTask() {
        final Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Run every minute
                    cleanupExpiredLocks();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "AntiDupeGuard-Cleanup");

        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * Cleanup expired locks.
     */
    private void cleanupExpiredLocks() {
        final long now = System.currentTimeMillis();
        final long timeout = SecurityConfig.ANTIDUPE_LOCK_TIMEOUT * 2; // Double timeout for safety

        int removed = 0;

        for (var entry : _locks.entrySet()) {
            final TransactionLock lock = entry.getValue();

            // Remove locks that haven't been accessed recently and are not held
            if (!lock.lock.isLocked() && (now - lock.lastAccessTime) > timeout) {
                _locks.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            _log.info(String.format("Cleaned up %d expired transaction locks", removed));
        }
    }

    /**
     * Get statistics.
     * 
     * @return Statistics string
     */
    public String getStats() {
        return String.format("AntiDupeGuard Stats - Active Locks: %d, Active Transactions: %d, Total Transactions: %d",
                _locks.size(), _activeTransactions.size(), _transactionSequence.get());
    }
}
