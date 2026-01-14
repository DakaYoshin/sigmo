# SIGMO Security System Architecture

## Overview
This document describes the comprehensive security system designed for the SIGMO L2J Interlude server. The system is designed to prevent modern exploits and cheats while maintaining compatibility with the existing L2J Frozen architecture.

## Architecture Principles

### 1. Server-Side Only
- All security checks are performed server-side
- No client modifications required
- Zero trust approach to client data

### 2. Non-Invasive Integration
- Integrates with existing systems without core rewrites
- Uses hook points in existing handlers
- Minimal performance overhead

### 3. Modern Java 17
- Utilizes concurrent collections
- Lock-free algorithms where possible
- Structured concurrency patterns

## Module Structure

```
com.gameserver.security/
├── core/
│   ├── SecurityManager.java           # Main coordinator
│   ├── SecurityConfig.java            # Configuration
│   └── SecurityLogger.java            # Centralized logging
├── antidupe/
│   ├── AntiDupeGuard.java            # Item duplication prevention
│   ├── TransactionLock.java          # Transaction locking mechanism
│   └── ItemSequenceValidator.java    # Sequence validation
├── firewall/
│   ├── PacketFirewall.java           # Packet validation
│   ├── PacketRateLimiter.java        # Rate limiting
│   └── PacketSequenceValidator.java  # Opcode sequence validation
├── validator/
│   ├── ActionValidator.java          # Player action validation
│   ├── SpeedValidator.java           # Speed check
│   └── SkillValidator.java           # Skill reuse validation
├── detector/
│   ├── BotDetector.java              # Bot detection
│   ├── PatternAnalyzer.java          # Pattern analysis
│   └── BehaviorScorer.java           # Heuristic scoring
└── audit/
    ├── SecurityAudit.java            # Audit logging
    ├── AuditDatabase.java            # Database persistence
    └── AuditFileLogger.java          # File logging
```

## Component Details

### 1. AntiDupeGuard
**Purpose**: Prevent item duplication exploits

**Key Features**:
- Transaction-level locking using `ReentrantLock`
- Unique transaction IDs for all item operations
- Server-side sequence validation
- Rollback mechanism for failed transactions

**Protected Operations**:
- Trade (give/receive)
- Drop item
- Destroy item
- Warehouse deposit/withdraw
- Mail send/receive
- Enchant/Augment

**Lock Strategy**:
```
Lock Key: charId + itemObjectId + operationType
Timeout: 5 seconds
Cleanup: Automatic on timeout or completion
```

### 2. PacketFirewall
**Purpose**: Detect and prevent packet injection attacks

**Key Features**:
- Per-client packet rate limiting
- Opcode sequence validation
- Packet size anomaly detection
- Progressive punishment system

**Rate Limits**:
- General packets: 100/second
- Movement packets: 20/second
- Action packets: 10/second
- Trade packets: 5/second

**Punishment Levels**:
1. **Warning**: Log and monitor (threshold: 3 violations/minute)
2. **Throttle**: Delay packet processing (threshold: 5 violations/minute)
3. **Kick**: Disconnect client (threshold: 10 violations/minute)
4. **Ban**: Temporary ban (threshold: 15 violations/minute)

### 3. ActionValidator
**Purpose**: Validate player actions against server formulas

**Key Features**:
- Attack speed validation
- Skill reuse time validation
- Movement speed validation
- Cast time validation

**Validation Strategy**:
- Calculate expected values server-side
- Compare with client-reported values
- Allow 5% tolerance for network latency
- Progressive scoring system

**Checks**:
- Attack speed: Based on weapon type, buffs, stats
- Move speed: Based on class, buffs, terrain
- Skill reuse: Based on skill data, cooldown reduction
- Cast time: Based on skill data, casting speed buffs

### 4. BotDetector
**Purpose**: Detect automated gameplay (bots)

**Key Features**:
- Pattern recognition
- Timing analysis
- Path repetition detection
- Heuristic scoring (0-100)

**Detection Patterns**:
- **Fixed Timing**: Actions at exact intervals (±10ms)
- **Path Repetition**: Same path >5 times
- **24/7 Activity**: >20 hours without logout
- **Inhuman Reactions**: Response time <50ms
- **Perfect Targeting**: Always targets optimal mob

**Scoring System**:
```
Score 0-30:   Normal player
Score 31-60:  Suspicious (monitor)
Score 61-80:  Likely bot (flag for GM review)
Score 81-100: Confirmed bot (auto-kick, log)
```

### 5. SecurityAudit
**Purpose**: Comprehensive logging of security events

**Key Features**:
- Dual logging (database + file)
- Async logging to prevent performance impact
- Automatic log rotation
- GM action tracking

**Logged Events**:
- Item creation/deletion
- GM commands
- Admin panel actions
- Security violations
- Ban/kick actions
- Trade transactions >1M adena
- Warehouse transfers >5M adena

**Log Retention**:
- Database: 90 days
- Files: 365 days (compressed after 30 days)

## Database Schema

### security_audit
```sql
CREATE TABLE IF NOT EXISTS security_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(50) NOT NULL,
    player_id INT,
    player_name VARCHAR(50),
    account_name VARCHAR(50),
    ip_address VARCHAR(45),
    details TEXT,
    severity ENUM('INFO', 'WARNING', 'CRITICAL'),
    INDEX idx_timestamp (timestamp),
    INDEX idx_player_id (player_id),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### security_violations
```sql
CREATE TABLE IF NOT EXISTS security_violations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    player_id INT NOT NULL,
    violation_type VARCHAR(50) NOT NULL,
    violation_count INT DEFAULT 1,
    last_violation TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    punishment_level TINYINT DEFAULT 0,
    is_banned BOOLEAN DEFAULT FALSE,
    ban_expiry TIMESTAMP NULL,
    details TEXT,
    INDEX idx_player_id (player_id),
    INDEX idx_violation_type (violation_type),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### bot_detection_scores
```sql
CREATE TABLE IF NOT EXISTS bot_detection_scores (
    player_id INT PRIMARY KEY,
    score INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    pattern_violations INT DEFAULT 0,
    timing_violations INT DEFAULT 0,
    path_violations INT DEFAULT 0,
    flagged_for_review BOOLEAN DEFAULT FALSE,
    review_notes TEXT,
    INDEX idx_score (score),
    INDEX idx_flagged (flagged_for_review)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Integration Points

### 1. Packet Handler Integration
**File**: `com.gameserver.network.L2GamePacketHandler`

**Integration**:
```java
// In handlePacket() method, before packet processing
if (!PacketFirewall.getInstance().validatePacket(client, opcode, buf.remaining())) {
    return null; // Packet rejected
}
```

### 2. Trade Controller Integration
**File**: `com.gameserver.TradeController`

**Integration**:
```java
// Before trade execution
if (!AntiDupeGuard.getInstance().acquireItemLock(player, item, "TRADE")) {
    player.sendMessage("Transaction in progress, please wait.");
    return;
}
try {
    // Execute trade
} finally {
    AntiDupeGuard.getInstance().releaseItemLock(player, item, "TRADE");
}
```

### 3. Inventory Integration
**File**: `com.gameserver.model.itemcontainer.PcInventory`

**Integration**:
```java
// In destroyItem(), dropItem(), transferItem()
if (!AntiDupeGuard.getInstance().validateItemOperation(player, item, operationType)) {
    SecurityAudit.getInstance().logSuspiciousActivity(player, "Invalid item operation");
    return false;
}
```

### 4. Warehouse Integration
**File**: `com.gameserver.model.itemcontainer.Warehouse`

**Integration**:
```java
// In depositItem() and withdrawItem()
if (!AntiDupeGuard.getInstance().acquireItemLock(player, item, "WAREHOUSE")) {
    return;
}
try {
    // Execute warehouse operation
} finally {
    AntiDupeGuard.getInstance().releaseItemLock(player, item, "WAREHOUSE");
}
```

### 5. Player Action Integration
**File**: `com.gameserver.model.actor.instance.L2PcInstance`

**Integration**:
```java
// In doAttack(), doCast(), etc.
if (!ActionValidator.getInstance().validateAction(this, actionType, actionTime)) {
    SecurityAudit.getInstance().logViolation(this, "Action speed violation");
    return;
}
```

## Configuration

### security.properties
```properties
# Anti-Dupe Configuration
antidupe.enabled = true
antidupe.lock.timeout = 5000
antidupe.log.all.operations = false

# Packet Firewall Configuration
firewall.enabled = true
firewall.rate.general = 100
firewall.rate.movement = 20
firewall.rate.action = 10
firewall.rate.trade = 5
firewall.auto.punish = true

# Action Validator Configuration
validator.enabled = true
validator.tolerance.percent = 5
validator.check.attack.speed = true
validator.check.move.speed = true
validator.check.skill.reuse = true

# Bot Detector Configuration
botdetector.enabled = true
botdetector.score.threshold.monitor = 31
botdetector.score.threshold.flag = 61
botdetector.score.threshold.kick = 81
botdetector.auto.kick = false

# Security Audit Configuration
audit.enabled = true
audit.log.to.database = true
audit.log.to.file = true
audit.log.gm.commands = true
audit.retention.days = 90
```

## Performance Considerations

### Memory Usage
- **PacketFirewall**: ~1KB per active client
- **AntiDupeGuard**: ~500 bytes per active transaction
- **BotDetector**: ~2KB per monitored player
- **Total Overhead**: ~10-20MB for 1000 concurrent players

### CPU Usage
- **Packet Validation**: <1ms per packet
- **Action Validation**: <0.5ms per action
- **Bot Detection**: Runs every 60 seconds, <100ms
- **Total Overhead**: <2% CPU usage

### Database Impact
- **Audit Logging**: Async, batched writes
- **Violation Tracking**: Cached, periodic sync
- **Bot Scores**: Updated every 60 seconds

## Monitoring & Alerts

### Admin Commands
```
//security status - View security system status
//security violations [player] - View player violations
//security botscore [player] - View bot detection score
//security audit [type] [hours] - View audit log
//security whitelist [player] - Whitelist player from checks
```

### Metrics Dashboard
- Active violations per minute
- Top violators
- Bot detection scores distribution
- Packet rejection rate
- System performance metrics

## Testing Strategy

### Unit Tests
- Transaction locking mechanism
- Rate limiter accuracy
- Sequence validator logic
- Bot scoring algorithm

### Integration Tests
- Trade flow with anti-dupe
- Packet flood scenarios
- Speed hack detection
- Bot pattern recognition

### Load Tests
- 1000 concurrent players
- 10,000 packets/second
- 100 simultaneous trades

## Deployment Checklist

1. ✓ Create database tables
2. ✓ Add security.properties to config
3. ✓ Compile security module
4. ✓ Integrate with packet handler
5. ✓ Integrate with trade controller
6. ✓ Integrate with inventory system
7. ✓ Integrate with warehouse system
8. ✓ Test in development environment
9. ✓ Monitor for false positives
10. ✓ Deploy to production

## Known Limitations

1. **Network Latency**: May cause false positives for high-latency players (>200ms)
2. **Bot Detection**: Heuristic-based, may flag skilled players
3. **Performance**: Heavy load (>2000 players) may require tuning
4. **Compatibility**: Tested with L2J Frozen/Interlude only

## Future Enhancements

1. Machine learning-based bot detection
2. IP-based geolocation blocking
3. Hardware ID tracking
4. Advanced pattern recognition
5. Integration with external anti-cheat services

## Support & Maintenance

- Review security logs weekly
- Update bot detection patterns monthly
- Tune rate limits based on server population
- Monitor false positive rate
- Keep violation database optimized

---

**Version**: 1.0.0  
**Last Updated**: 2026-01-13  
**Author**: SIGMO Security Team  
**License**: GNU GPL v2
