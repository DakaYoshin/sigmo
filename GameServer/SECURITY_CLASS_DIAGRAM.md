# SIGMO Security System - Class Diagram

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      SecurityManager                             │
│  (Main Coordinator - Singleton)                                  │
│                                                                   │
│  + initialize()                                                   │
│  + shutdown()                                                     │
│  + onPlayerLogin(player)                                          │
│  + onPlayerLogout(player)                                         │
│  + getStats()                                                     │
└────────────┬────────────────────────────────────────────────────┘
             │
             │ manages
             │
    ┌────────┴────────┬──────────┬──────────┬──────────┬──────────┐
    │                 │          │          │          │          │
    ▼                 ▼          ▼          ▼          ▼          ▼
┌─────────┐   ┌─────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐
│Security │   │ AntiDupe    │ │  Packet    │ │  Action    │ │   Bot    │
│Config   │   │   Guard     │ │  Firewall  │ │ Validator  │ │ Detector │
└─────────┘   └─────────────┘ └────────────┘ └────────────┘ └──────────┘
                     │                │              │              │
                     │                │              │              │
                     ▼                ▼              ▼              ▼
              ┌─────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
              │Transaction  │  │  Rate    │  │  Speed   │  │ Pattern  │
              │   Lock      │  │ Limiter  │  │Validator │  │ Analyzer │
              └─────────────┘  └──────────┘  └──────────┘  └──────────┘

    ┌──────────────────────────────────────────────────────────────┐
    │                    SecurityAudit                              │
    │  (Audit Logging - Singleton)                                  │
    │                                                                │
    │  + logItemCreation(player, itemId, count, method)             │
    │  + logItemDeletion(player, itemId, count, method)             │
    │  + logGMCommand(player, command, target)                      │
    │  + logTrade(player1, player2, adena)                          │
    │  + logWarehouse(player, operation, adena)                     │
    │  + logViolation(player, type, details)                        │
    └────────────────────────────┬─────────────────────────────────┘
                                 │
                                 │ uses
                                 │
                        ┌────────┴────────┐
                        │                 │
                        ▼                 ▼
                ┌──────────────┐  ┌──────────────┐
                │   Database   │  │SecurityLogger│
                │   Writer     │  │ (File Writer)│
                └──────────────┘  └──────────────┘
```

---

## Core Components

### 1. SecurityManager

**Package**: `com.gameserver.security.core`

**Responsibilities**:
- Initialize all security subsystems
- Coordinate security operations
- Handle player lifecycle events
- Provide system statistics

**Key Methods**:
```java
+ initialize(): void
+ shutdown(): void
+ onPlayerLogin(L2PcInstance): void
+ onPlayerLogout(L2PcInstance): void
+ onPlayerDisconnect(L2PcInstance): void
+ printStatus(): void
+ getStats(): String
+ isInitialized(): boolean
```

**Dependencies**:
- SecurityConfig
- SecurityLogger
- AntiDupeGuard
- PacketFirewall
- ActionValidator
- BotDetector
- SecurityAudit

---

### 2. SecurityConfig

**Package**: `com.gameserver.security.core`

**Responsibilities**:
- Load configuration from file
- Provide static access to settings
- Manage default values

**Key Fields**:
```java
// Anti-Dupe
+ ANTIDUPE_ENABLED: boolean
+ ANTIDUPE_LOCK_TIMEOUT: int
+ ANTIDUPE_LOG_ALL_OPERATIONS: boolean

// Firewall
+ FIREWALL_ENABLED: boolean
+ FIREWALL_RATE_GENERAL: int
+ FIREWALL_RATE_MOVEMENT: int
+ FIREWALL_RATE_ACTION: int
+ FIREWALL_RATE_TRADE: int
+ FIREWALL_AUTO_PUNISH: boolean

// Validator
+ VALIDATOR_ENABLED: boolean
+ VALIDATOR_TOLERANCE_PERCENT: int
+ VALIDATOR_CHECK_ATTACK_SPEED: boolean
+ VALIDATOR_CHECK_MOVE_SPEED: boolean

// Bot Detector
+ BOTDETECTOR_ENABLED: boolean
+ BOTDETECTOR_SCORE_THRESHOLD_MONITOR: int
+ BOTDETECTOR_SCORE_THRESHOLD_FLAG: int
+ BOTDETECTOR_SCORE_THRESHOLD_KICK: int

// Audit
+ AUDIT_ENABLED: boolean
+ AUDIT_LOG_TO_DATABASE: boolean
+ AUDIT_LOG_TO_FILE: boolean
```

**Key Methods**:
```java
+ load(): void
- loadDefaults(): void
```

---

### 3. SecurityLogger

**Package**: `com.gameserver.security.core`

**Responsibilities**:
- Async file logging
- Queue management
- Log rotation

**Key Methods**:
```java
+ log(category, message): void
+ logf(category, format, args): void
+ shutdown(): void
+ getQueueSize(): int
+ run(): void (Runnable)
```

**Internal Classes**:
```java
- LogEntry
  - category: String
  - message: String
  - timestamp: long
```

---

## Anti-Dupe Module

### 4. AntiDupeGuard

**Package**: `com.gameserver.security.antidupe`

**Responsibilities**:
- Transaction locking
- Item operation validation
- Sequence tracking
- Lock cleanup

**Key Methods**:
```java
+ acquireItemLock(player, item, operationType): boolean
+ releaseItemLock(player, item, operationType): void
+ validateItemOperation(player, item, operationType): boolean
+ releaseAllLocks(player): void
+ getStats(): String
```

**Internal Classes**:
```java
- TransactionLock
  - lock: ReentrantLock
  - createdTime: long
  - lastAccessTime: long

- TransactionInfo
  - transactionId: long
  - operationType: String
  - itemObjectId: int
  - startTime: long
```

**Data Structures**:
```java
- _locks: ConcurrentHashMap<String, TransactionLock>
- _transactionSequence: AtomicLong
- _activeTransactions: ConcurrentHashMap<Integer, TransactionInfo>
```

---

## Firewall Module

### 5. PacketFirewall

**Package**: `com.gameserver.security.firewall`

**Responsibilities**:
- Packet rate limiting
- Packet validation
- Sequence checking
- Progressive punishment

**Key Methods**:
```java
+ validatePacket(client, opcode, size): boolean
+ removeClient(client): void
+ getStats(): String
- categorizePacket(opcode): PacketCategory
- checkRateLimit(client, stats, category, count, opcode): boolean
- checkPacketSize(client, stats, opcode, size): boolean
- checkPacketSequence(client, stats, opcode): boolean
- handleViolation(client, stats, type, details): void
- applyPunishment(client, stats, violations): void
```

**Internal Classes**:
```java
- ClientPacketStats
  - clientId: String
  - generalPackets: AtomicLong
  - movementPackets: AtomicLong
  - actionPackets: AtomicLong
  - tradePackets: AtomicLong
  - violations: AtomicInteger
  - punishmentLevel: AtomicInteger
  - lastResetTime: long
  - lastViolationTime: long
  - lastOpcode: int
```

**Enums**:
```java
- PacketCategory
  - GENERAL
  - MOVEMENT
  - ACTION
  - TRADE
```

---

## Validator Module

### 6. ActionValidator

**Package**: `com.gameserver.security.validator`

**Responsibilities**:
- Attack speed validation
- Movement speed validation
- Skill reuse validation
- Cast time validation

**Key Methods**:
```java
+ validateAttack(player): boolean
+ validateMovement(player, distance, timeDelta): boolean
+ validateSkillUse(player, skill): boolean
+ validateCastTime(player, skill, castTime): boolean
+ removePlayer(player): void
+ getPlayerViolations(player): int
+ getStats(): String
- calculateAttackDelay(player): int
- calculateMoveSpeed(player): double
- calculateCastTime(player, skill): int
```

**Internal Classes**:
```java
- PlayerActionTracker
  - playerId: int
  - lastAttackTime: long
  - lastMoveTime: long
  - lastSkillTime: long
  - lastCastTime: long
  - attackSpeedViolations: AtomicInteger
  - moveSpeedViolations: AtomicInteger
  - skillReuseViolations: AtomicInteger
  - castTimeViolations: AtomicInteger
```

---

## Detector Module

### 7. BotDetector

**Package**: `com.gameserver.security.detector`

**Responsibilities**:
- Pattern recognition
- Timing analysis
- Path tracking
- Heuristic scoring

**Key Methods**:
```java
+ trackAction(player): void
+ trackMovement(player, x, y, z): void
+ getPlayerScore(player): int
+ isPlayerFlagged(player): boolean
+ clearPlayerFlag(player): void
+ removePlayer(player): void
+ getStats(): String
+ run(): void (Runnable)
- analyzePlayerBehavior(tracker): void
- analyzeTimingPatterns(tracker): int
- analyzePathPatterns(tracker): int
- analyzePlayTime(tracker): int
```

**Internal Classes**:
```java
- BehaviorTracker
  - playerId: int
  - botScore: AtomicInteger
  - pathHistory: List<Point3D>
  - actionTimings: List<Long>
  - loginTime: long
  - lastActionTime: long
  - totalPlayTime: long
  - flaggedForReview: boolean
  - pathRepetitions: int
  - fixedTimingCount: int
  - perfectTargetingCount: int
```

---

## Audit Module

### 8. SecurityAudit

**Package**: `com.gameserver.security.audit`

**Responsibilities**:
- Event logging
- Database persistence
- Audit trail
- Compliance

**Key Methods**:
```java
+ logItemCreation(player, itemId, count, method): void
+ logItemDeletion(player, itemId, count, method): void
+ logGMCommand(player, command, target): void
+ logTrade(player1, player2, adena): void
+ logWarehouse(player, operation, adena): void
+ logViolation(player, type, details): void
+ logBan(player, admin, reason, duration): void
+ shutdown(): void
+ getQueueSize(): int
+ getStats(): String
+ run(): void (Runnable)
```

**Internal Classes**:
```java
- AuditEvent
  - eventType: String
  - playerId: int
  - playerName: String
  - accountName: String
  - ipAddress: String
  - details: String
  - severity: Severity
  - timestamp: long
```

**Enums**:
```java
- Severity
  - INFO
  - WARNING
  - CRITICAL
```

---

## Data Flow Diagrams

### Trade Flow with Anti-Dupe

```
Player initiates trade
        │
        ▼
┌───────────────────┐
│  TradeController  │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  AntiDupeGuard    │
│  acquireItemLock()│
└────────┬──────────┘
         │
    ┌────┴────┐
    │         │
   YES       NO
    │         │
    │         └──► Send error message
    │              Return
    ▼
Execute trade
    │
    ▼
┌───────────────────┐
│  SecurityAudit    │
│  logTrade()       │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  AntiDupeGuard    │
│  releaseItemLock()│
└───────────────────┘
```

### Packet Validation Flow

```
Packet received
        │
        ▼
┌───────────────────┐
│ PacketFirewall    │
│ validatePacket()  │
└────────┬──────────┘
         │
    ┌────┴────┐
    │         │
   YES       NO
    │         │
    │         └──► Log violation
    │              Apply punishment
    │              Return null
    ▼
Process packet normally
```

### Bot Detection Flow

```
Player action
        │
        ▼
┌───────────────────┐
│  BotDetector      │
│  trackAction()    │
└────────┬──────────┘
         │
         ▼
Store timing data
         │
         ▼
Periodic scan (60s)
         │
         ▼
┌───────────────────┐
│  BotDetector      │
│  analyzePatterns()│
└────────┬──────────┘
         │
         ▼
Calculate score
         │
    ┌────┴────┐
    │         │
  HIGH      LOW
    │         │
    │         └──► Continue monitoring
    ▼
Flag for review
Auto-kick (if enabled)
```

---

## Thread Safety

All components are designed to be thread-safe:

1. **Singleton Pattern**: Double-checked locking
2. **Concurrent Collections**: ConcurrentHashMap for all shared data
3. **Atomic Operations**: AtomicLong, AtomicInteger for counters
4. **Lock Management**: ReentrantLock for transaction locking
5. **Async Processing**: Separate threads for logging and detection

---

## Memory Footprint

Estimated memory usage per 1000 concurrent players:

| Component | Memory Usage |
|-----------|-------------|
| AntiDupeGuard | ~500 KB |
| PacketFirewall | ~1 MB |
| ActionValidator | ~800 KB |
| BotDetector | ~2 MB |
| SecurityAudit | ~200 KB (queue) |
| SecurityLogger | ~100 KB (queue) |
| **Total** | **~5 MB** |

---

## Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| acquireItemLock() | O(1) | HashMap lookup + lock |
| validatePacket() | O(1) | Counter increment + comparison |
| validateAttack() | O(1) | Calculation + comparison |
| trackAction() | O(1) | List append (amortized) |
| analyzePatterns() | O(n²) | n = history size (limited to 20) |
| logEvent() | O(1) | Queue offer |

---

**Version**: 1.0.0  
**Last Updated**: 2026-01-13  
**Author**: SIGMO Security Team
