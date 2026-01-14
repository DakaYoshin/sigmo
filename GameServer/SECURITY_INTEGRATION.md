# SIGMO Security System - Integration Guide

## Table of Contents
1. [Installation](#installation)
2. [Integration Points](#integration-points)
3. [Code Examples](#code-examples)
4. [Testing](#testing)
5. [Troubleshooting](#troubleshooting)

---

## Installation

### Step 1: Database Setup

Run the SQL installation script on your MariaDB database:

```bash
mysql -u root -p your_database < GameServer/sql/security_system_install.sql
```

This will create the following tables:
- `security_audit`
- `security_violations`
- `bot_detection_scores`
- `item_transaction_log`
- `packet_firewall_stats`

### Step 2: Configuration

The configuration file is located at:
```
GameServer/config/security.properties
```

Review and adjust settings according to your server's needs.

### Step 3: Compile

Compile the security module along with your game server:

```bash
# If using Ant
ant compile

# If using Gradle
gradle build

# If using Maven
mvn compile
```

---

## Integration Points

### 1. GameServer Initialization

**File**: `com.gameserver.GameServer` (or your main server class)

**Location**: In the `main()` method or server initialization

```java
import com.gameserver.security.core.SecurityManager;

public class GameServer {
    public static void main(String[] args) {
        // ... existing initialization code ...
        
        // Initialize security system
        _log.info("Initializing security system...");
        SecurityManager.getInstance().initialize();
        
        // ... rest of initialization ...
    }
}
```

**Shutdown Hook**:

```java
// In shutdown method
public void shutdown() {
    // ... existing shutdown code ...
    
    // Shutdown security system
    SecurityManager.getInstance().shutdown();
    
    // ... rest of shutdown ...
}
```

---

### 2. Packet Handler Integration

**File**: `com.gameserver.network.L2GamePacketHandler.java`

**Method**: `handlePacket(ByteBuffer buf, L2GameClient client)`

**Add at the beginning of the method** (after state checks):

```java
import com.gameserver.security.firewall.PacketFirewall;

@Override
public ReceivablePacket<L2GameClient> handlePacket(ByteBuffer buf, L2GameClient client) {
    // ... existing state checks ...
    
    // Security: Validate packet
    if (!PacketFirewall.getInstance().validatePacket(client, opcode, buf.remaining())) {
        return null; // Packet rejected by firewall
    }
    
    // ... rest of packet handling ...
}
```

**On client disconnect**:

```java
// In L2GameClient.java or connection handler
import com.gameserver.security.firewall.PacketFirewall;

public void onDisconnection() {
    // ... existing disconnect code ...
    
    // Remove client from firewall
    PacketFirewall.getInstance().removeClient(this);
    
    // ... rest of disconnect handling ...
}
```

---

### 3. Player Login/Logout Integration

**File**: `com.gameserver.model.actor.instance.L2PcInstance.java` or `EnterWorld.java`

**On player login**:

```java
import com.gameserver.security.core.SecurityManager;

// In login/enter world method
public void onPlayerLogin(L2PcInstance player) {
    // ... existing login code ...
    
    // Notify security system
    SecurityManager.getInstance().onPlayerLogin(player);
    
    // ... rest of login ...
}
```

**On player logout**:

```java
// In logout method
public void onPlayerLogout(L2PcInstance player) {
    // ... existing logout code ...
    
    // Notify security system
    SecurityManager.getInstance().onPlayerLogout(player);
    
    // ... rest of logout ...
}
```

---

### 4. Trade System Integration

**File**: `com.gameserver.network.clientpackets.TradeDone.java` or `TradeController.java`

**Before trade execution**:

```java
import com.gameserver.security.antidupe.AntiDupeGuard;
import com.gameserver.security.audit.SecurityAudit;

public void executeTrade(L2PcInstance player1, L2PcInstance player2) {
    // Get items being traded
    for (TradeItem item : player1.getTradeList().getItems()) {
        L2ItemInstance itemInstance = player1.getInventory().getItemByObjectId(item.getObjectId());
        
        // Acquire lock
        if (!AntiDupeGuard.getInstance().acquireItemLock(player1, itemInstance, "TRADE")) {
            player1.sendMessage("Transaction in progress, please wait.");
            return;
        }
    }
    
    try {
        // Execute trade
        // ... your existing trade code ...
        
        // Audit log (if large trade)
        long adenaAmount = calculateAdenaAmount(player1, player2);
        SecurityAudit.getInstance().logTrade(player1, player2, adenaAmount);
        
    } finally {
        // Release locks
        for (TradeItem item : player1.getTradeList().getItems()) {
            L2ItemInstance itemInstance = player1.getInventory().getItemByObjectId(item.getObjectId());
            AntiDupeGuard.getInstance().releaseItemLock(player1, itemInstance, "TRADE");
        }
    }
}
```

---

### 5. Inventory Operations Integration

**File**: `com.gameserver.model.itemcontainer.PcInventory.java`

**In destroyItem() method**:

```java
import com.gameserver.security.antidupe.AntiDupeGuard;
import com.gameserver.security.audit.SecurityAudit;

public L2ItemInstance destroyItem(String process, int objectId, long count, L2PcInstance actor, Object reference) {
    L2ItemInstance item = getItemByObjectId(objectId);
    
    if (item == null) {
        return null;
    }
    
    // Validate operation
    if (!AntiDupeGuard.getInstance().validateItemOperation(actor, item, "DESTROY")) {
        SecurityAudit.getInstance().logViolation(actor, "INVALID_DESTROY", 
            "Attempted to destroy item " + item.getObjectId());
        return null;
    }
    
    // Acquire lock
    if (!AntiDupeGuard.getInstance().acquireItemLock(actor, item, "DESTROY")) {
        return null;
    }
    
    try {
        // ... existing destroy code ...
        
        return destroyedItem;
        
    } finally {
        AntiDupeGuard.getInstance().releaseItemLock(actor, item, "DESTROY");
    }
}
```

**In dropItem() method**:

```java
public L2ItemInstance dropItem(String process, int objectId, long count, int x, int y, int z, L2PcInstance actor, Object reference) {
    L2ItemInstance item = getItemByObjectId(objectId);
    
    if (item == null) {
        return null;
    }
    
    // Validate and lock
    if (!AntiDupeGuard.getInstance().validateItemOperation(actor, item, "DROP")) {
        return null;
    }
    
    if (!AntiDupeGuard.getInstance().acquireItemLock(actor, item, "DROP")) {
        return null;
    }
    
    try {
        // ... existing drop code ...
        
        return droppedItem;
        
    } finally {
        AntiDupeGuard.getInstance().releaseItemLock(actor, item, "DROP");
    }
}
```

---

### 6. Warehouse Integration

**File**: `com.gameserver.model.itemcontainer.Warehouse.java`

**In depositItem() method**:

```java
import com.gameserver.security.antidupe.AntiDupeGuard;
import com.gameserver.security.audit.SecurityAudit;

public L2ItemInstance depositItem(L2PcInstance actor, int objectId, long count) {
    L2ItemInstance item = actor.getInventory().getItemByObjectId(objectId);
    
    if (item == null) {
        return null;
    }
    
    // Acquire lock
    if (!AntiDupeGuard.getInstance().acquireItemLock(actor, item, "WAREHOUSE_DEPOSIT")) {
        actor.sendMessage("Transaction in progress, please wait.");
        return null;
    }
    
    try {
        // ... existing deposit code ...
        
        // Audit log if adena
        if (item.getItemId() == 57 && count >= SecurityConfig.AUDIT_WAREHOUSE_THRESHOLD) {
            SecurityAudit.getInstance().logWarehouse(actor, "DEPOSIT", count);
        }
        
        return depositedItem;
        
    } finally {
        AntiDupeGuard.getInstance().releaseItemLock(actor, item, "WAREHOUSE_DEPOSIT");
    }
}
```

**In withdrawItem() method**:

```java
public L2ItemInstance withdrawItem(L2PcInstance actor, int objectId, long count) {
    L2ItemInstance item = getItemByObjectId(objectId);
    
    if (item == null) {
        return null;
    }
    
    // Acquire lock
    if (!AntiDupeGuard.getInstance().acquireItemLock(actor, item, "WAREHOUSE_WITHDRAW")) {
        actor.sendMessage("Transaction in progress, please wait.");
        return null;
    }
    
    try {
        // ... existing withdraw code ...
        
        // Audit log if adena
        if (item.getItemId() == 57 && count >= SecurityConfig.AUDIT_WAREHOUSE_THRESHOLD) {
            SecurityAudit.getInstance().logWarehouse(actor, "WITHDRAW", count);
        }
        
        return withdrawnItem;
        
    } finally {
        AntiDupeGuard.getInstance().releaseItemLock(actor, item, "WAREHOUSE_WITHDRAW");
    }
}
```

---

### 7. Action Validation Integration

**File**: `com.gameserver.model.actor.instance.L2PcInstance.java`

**In doAttack() method**:

```java
import com.gameserver.security.validator.ActionValidator;
import com.gameserver.security.detector.BotDetector;

public void doAttack(L2Character target) {
    // Validate attack speed
    if (!ActionValidator.getInstance().validateAttack(this)) {
        // Attack rejected - too fast
        return;
    }
    
    // Track action for bot detection
    BotDetector.getInstance().trackAction(this);
    
    // ... existing attack code ...
}
```

**In doCast() method**:

```java
public void doCast(L2Skill skill) {
    // Validate skill reuse
    if (!ActionValidator.getInstance().validateSkillUse(this, skill)) {
        sendMessage("Skill is not ready yet.");
        return;
    }
    
    // Track action
    BotDetector.getInstance().trackAction(this);
    
    // ... existing cast code ...
}
```

**In movement handler**:

```java
// In MoveBackwardToLocation.java or movement packet handler
import com.gameserver.security.validator.ActionValidator;
import com.gameserver.security.detector.BotDetector;

public void moveToLocation(int x, int y, int z) {
    // Calculate distance and time
    double distance = calculateDistance(currentX, currentY, x, y);
    long timeDelta = System.currentTimeMillis() - lastMoveTime;
    
    // Validate movement speed
    if (!ActionValidator.getInstance().validateMovement(this, distance, timeDelta)) {
        // Movement rejected - too fast (speed hack)
        sendMessage("Invalid movement detected.");
        return;
    }
    
    // Track movement for bot detection
    BotDetector.getInstance().trackMovement(this, x, y, z);
    
    // ... existing movement code ...
}
```

---

### 8. GM Command Integration

**File**: `com.gameserver.handler.admincommandhandlers.AdminCreateItem.java` (and similar)

```java
import com.gameserver.security.audit.SecurityAudit;

public void createItem(L2PcInstance admin, int itemId, long count) {
    // ... existing create item code ...
    
    // Audit log
    SecurityAudit.getInstance().logItemCreation(admin, itemId, count, "GM_COMMAND");
    
    // ... rest of code ...
}
```

**For all GM commands**:

```java
import com.gameserver.security.audit.SecurityAudit;

public boolean useAdminCommand(String command, L2PcInstance activeChar) {
    // ... existing command handling ...
    
    // Audit log
    String target = extractTarget(command); // Your logic to extract target
    SecurityAudit.getInstance().logGMCommand(activeChar, command, target);
    
    // ... rest of code ...
}
```

---

## Code Examples

### Example 1: Complete Trade Integration

```java
package com.gameserver.model;

import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.items.instance.L2ItemInstance;
import com.gameserver.security.antidupe.AntiDupeGuard;
import com.gameserver.security.audit.SecurityAudit;

public class TradeManager {
    
    public boolean executeTrade(L2PcInstance player1, L2PcInstance player2) {
        // Collect all items from both players
        List<L2ItemInstance> allItems = new ArrayList<>();
        allItems.addAll(getTradeItems(player1));
        allItems.addAll(getTradeItems(player2));
        
        // Acquire all locks
        for (L2ItemInstance item : allItems) {
            L2PcInstance owner = item.getOwnerId() == player1.getObjectId() ? player1 : player2;
            
            if (!AntiDupeGuard.getInstance().acquireItemLock(owner, item, "TRADE")) {
                // Failed to acquire lock, release all previous locks
                releaseAllLocks(allItems, player1, player2);
                player1.sendMessage("Trade failed: transaction in progress.");
                player2.sendMessage("Trade failed: transaction in progress.");
                return false;
            }
        }
        
        try {
            // Execute trade
            boolean success = performTradeExchange(player1, player2);
            
            if (success) {
                // Audit log
                long adenaAmount = calculateAdenaAmount(player1, player2);
                SecurityAudit.getInstance().logTrade(player1, player2, adenaAmount);
            }
            
            return success;
            
        } finally {
            // Always release locks
            releaseAllLocks(allItems, player1, player2);
        }
    }
    
    private void releaseAllLocks(List<L2ItemInstance> items, L2PcInstance p1, L2PcInstance p2) {
        for (L2ItemInstance item : items) {
            L2PcInstance owner = item.getOwnerId() == p1.getObjectId() ? p1 : p2;
            AntiDupeGuard.getInstance().releaseItemLock(owner, item, "TRADE");
        }
    }
}
```

### Example 2: Admin Command with Audit

```java
package com.gameserver.handler.admincommandhandlers;

import com.gameserver.handler.IAdminCommandHandler;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.security.audit.SecurityAudit;

public class AdminSecurity implements IAdminCommandHandler {
    
    private static final String[] ADMIN_COMMANDS = {
        "admin_security_status",
        "admin_security_violations",
        "admin_security_botscore"
    };
    
    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        // Log command
        SecurityAudit.getInstance().logGMCommand(activeChar, command, null);
        
        if (command.equals("admin_security_status")) {
            showSecurityStatus(activeChar);
        }
        else if (command.startsWith("admin_security_violations")) {
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                showPlayerViolations(activeChar, parts[1]);
            }
        }
        else if (command.startsWith("admin_security_botscore")) {
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                showBotScore(activeChar, parts[1]);
            }
        }
        
        return true;
    }
    
    private void showSecurityStatus(L2PcInstance admin) {
        String stats = SecurityManager.getInstance().getStats();
        admin.sendMessage("=== Security System Status ===");
        for (String line : stats.split("\n")) {
            admin.sendMessage(line);
        }
    }
    
    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}
```

---

## Testing

### Unit Testing

Create test cases for each security component:

```java
// Example test for AntiDupeGuard
public class AntiDupeGuardTest {
    
    @Test
    public void testConcurrentItemLock() {
        L2PcInstance player = createTestPlayer();
        L2ItemInstance item = createTestItem();
        
        // First lock should succeed
        assertTrue(AntiDupeGuard.getInstance().acquireItemLock(player, item, "TEST"));
        
        // Second lock on same item should fail
        assertFalse(AntiDupeGuard.getInstance().acquireItemLock(player, item, "TEST"));
        
        // Release lock
        AntiDupeGuard.getInstance().releaseItemLock(player, item, "TEST");
        
        // Now lock should succeed again
        assertTrue(AntiDupeGuard.getInstance().acquireItemLock(player, item, "TEST"));
    }
}
```

### Integration Testing

1. **Test Trade System**:
   - Attempt normal trade
   - Attempt duplicate trade (should be blocked)
   - Verify audit logs

2. **Test Packet Firewall**:
   - Send packets at normal rate (should pass)
   - Send packet flood (should be blocked)
   - Verify punishment system

3. **Test Bot Detection**:
   - Simulate bot-like behavior
   - Verify score increases
   - Check flagging system

---

## Troubleshooting

### Common Issues

#### 1. "Security system not initialized"

**Cause**: SecurityManager.initialize() not called

**Solution**: Add initialization call in GameServer main method

#### 2. "Lock timeout" messages

**Cause**: Lock timeout too short or deadlock

**Solution**: 
- Increase `antidupe.lock.timeout` in config
- Check for deadlocks in trade/warehouse code

#### 3. False positives in action validator

**Cause**: Tolerance too strict or formula mismatch

**Solution**:
- Increase `validator.tolerance.percent`
- Verify attack/cast speed formulas match your server

#### 4. Database connection errors

**Cause**: Tables not created or wrong database

**Solution**:
- Run SQL installation script
- Verify database connection in L2DatabaseFactory

#### 5. High memory usage

**Cause**: Too many tracked players or large queues

**Solution**:
- Reduce retention times
- Increase cleanup intervals
- Monitor queue sizes

---

## Performance Tuning

### For High Population Servers (>1000 players)

```properties
# Reduce logging verbosity
antidupe.log.all.operations = false
audit.log.trades = false

# Increase thresholds
audit.trade.threshold = 10000000
audit.warehouse.threshold = 50000000

# Reduce bot detection frequency
botdetector.scan.interval = 120000
```

### For Low Population Servers (<200 players)

```properties
# Enable detailed logging
antidupe.log.all.operations = true
audit.log.trades = true

# Lower thresholds
audit.trade.threshold = 100000
audit.warehouse.threshold = 500000

# Increase bot detection frequency
botdetector.scan.interval = 30000
```

---

## Support

For issues or questions:
1. Check logs in `GameServer/log/security/`
2. Review database audit tables
3. Use admin commands to check system status
4. Consult SECURITY_ARCHITECTURE.md for detailed information

---

**Version**: 1.0.0  
**Last Updated**: 2026-01-13  
**Author**: SIGMO Security Team
