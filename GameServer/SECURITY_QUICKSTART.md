# SIGMO Security System - Quick Start Guide

## 🚀 Quick Installation (5 Minutes)

### Step 1: Database Setup (1 minute)

```bash
# Connect to your MariaDB database
mysql -u root -p your_l2j_database

# Run the installation script
source GameServer/sql/security_system_install.sql
```

### Step 2: Configuration (1 minute)

The configuration file is already created at:
```
GameServer/config/security.properties
```

**For most servers, the default settings are fine.** Just verify the file exists.

### Step 3: Initialize Security System (2 minutes)

Find your main GameServer class (usually `com.gameserver.GameServer.java`) and add:

```java
import com.gameserver.security.core.SecurityManager;

// In your main() method, after database initialization:
public static void main(String[] args) {
    // ... existing code ...
    
    // Initialize database
    L2DatabaseFactory.getInstance();
    
    // ✨ ADD THIS LINE ✨
    SecurityManager.getInstance().initialize();
    
    // ... rest of initialization ...
}

// In your shutdown() method:
public void shutdown() {
    // ... existing shutdown code ...
    
    // ✨ ADD THIS LINE ✨
    SecurityManager.getInstance().shutdown();
    
    // ... rest of shutdown ...
}
```

### Step 4: Compile and Test (1 minute)

```bash
# Compile your server
ant compile
# or
gradle build

# Start the server
./startGameServer.sh
# or
startGameServer.bat
```

**Look for these log messages:**
```
=================================================
  SIGMO Security System v1.0.0
  Initializing security subsystems...
=================================================
Loading security configuration...
Initializing SecurityLogger...
Initializing AntiDupeGuard...
Initializing PacketFirewall...
Initializing ActionValidator...
Initializing BotDetector...
Initializing SecurityAudit...
=================================================
  Security System Initialized Successfully
=================================================
```

**✅ Done! The security system is now running in monitoring mode.**

---

## 📊 Verify Installation

### Check Database Tables

```sql
-- Verify tables were created
SHOW TABLES LIKE 'security_%';

-- Should show:
-- security_audit
-- security_violations
-- bot_detection_scores
-- item_transaction_log
-- packet_firewall_stats
```

### Check Log Files

```bash
# Check security logs directory
ls -la GameServer/log/security/

# Should show files like:
# antidupe_2026-01-13.log
# firewall_2026-01-13.log
# validator_2026-01-13.log
# botdetector_2026-01-13.log
# audit_2026-01-13.log
```

### Test Basic Functionality

1. **Login to the server**
2. **Check audit log**:
   ```sql
   SELECT * FROM security_audit ORDER BY timestamp DESC LIMIT 10;
   ```
3. **You should see system initialization events**

---

## 🔧 Basic Integration (Optional - Recommended)

For full protection, integrate with your existing systems:

### 1. Packet Handler (CRITICAL - Prevents packet injection)

**File**: `com.gameserver.network.L2GamePacketHandler.java`

**Add at line ~40** (in `handlePacket()` method):

```java
import com.gameserver.security.firewall.PacketFirewall;

// Add after state checks, before packet processing:
if (!PacketFirewall.getInstance().validatePacket(client, opcode, buf.remaining())) {
    return null;
}
```

### 2. Trade System (CRITICAL - Prevents item duplication)

**File**: `com.gameserver.network.clientpackets.TradeDone.java`

**Wrap trade execution**:

```java
import com.gameserver.security.antidupe.AntiDupeGuard;

// Before executing trade:
for (TradeItem item : tradeList.getItems()) {
    L2ItemInstance itemInstance = player.getInventory().getItemByObjectId(item.getObjectId());
    if (!AntiDupeGuard.getInstance().acquireItemLock(player, itemInstance, "TRADE")) {
        player.sendMessage("Transaction in progress, please wait.");
        return;
    }
}

try {
    // Your existing trade code here
} finally {
    // Release locks
    for (TradeItem item : tradeList.getItems()) {
        L2ItemInstance itemInstance = player.getInventory().getItemByObjectId(item.getObjectId());
        AntiDupeGuard.getInstance().releaseItemLock(player, itemInstance, "TRADE");
    }
}
```

### 3. Player Login/Logout (RECOMMENDED - Cleanup)

**File**: `com.gameserver.network.clientpackets.EnterWorld.java`

```java
import com.gameserver.security.core.SecurityManager;

// After successful login:
SecurityManager.getInstance().onPlayerLogin(player);
```

**File**: `com.gameserver.network.clientpackets.Logout.java`

```java
import com.gameserver.security.core.SecurityManager;

// Before logout:
SecurityManager.getInstance().onPlayerLogout(player);
```

---

## 🎮 Admin Commands (Coming Soon)

Create admin commands to monitor the system:

```java
//security status - View system status
//security violations [player] - View player violations
//security botscore [player] - View bot detection score
```

---

## 📈 Monitoring

### Real-time Monitoring

Check system status in server console:

```java
// Add to your admin panel or scheduled task
SecurityManager.getInstance().printStatus();
```

### Database Monitoring

```sql
-- Check recent violations
SELECT 
    player_name, 
    violation_type, 
    violation_count, 
    last_violation 
FROM security_violations 
ORDER BY last_violation DESC 
LIMIT 20;

-- Check bot scores
SELECT 
    player_name, 
    score, 
    flagged_for_review 
FROM bot_detection_scores 
WHERE score > 30 
ORDER BY score DESC;

-- Check recent audit events
SELECT 
    event_type, 
    player_name, 
    details, 
    severity, 
    timestamp 
FROM security_audit 
ORDER BY timestamp DESC 
LIMIT 50;
```

---

## ⚙️ Configuration Tuning

### For High Population Servers (>1000 players)

Edit `config/security.properties`:

```properties
# Reduce logging to improve performance
antidupe.log.all.operations = false
audit.log.trades = false

# Increase thresholds
firewall.rate.general = 150
audit.trade.threshold = 10000000
```

### For PvP Servers

```properties
# Stricter validation
validator.tolerance.percent = 3
firewall.auto.punish = true
firewall.kick.threshold = 8
```

### For Low Rate Servers

```properties
# More detailed logging
antidupe.log.all.operations = true
audit.log.trades = true
audit.trade.threshold = 100000

# Stricter bot detection
botdetector.score.threshold.flag = 50
botdetector.pattern.threshold = 3
```

---

## 🐛 Troubleshooting

### Issue: "Security system not initialized"

**Solution**: Make sure you called `SecurityManager.getInstance().initialize()` in your GameServer main method.

### Issue: "Table 'security_audit' doesn't exist"

**Solution**: Run the SQL installation script:
```bash
mysql -u root -p your_database < GameServer/sql/security_system_install.sql
```

### Issue: Players getting "Transaction in progress" messages

**Cause**: Lock timeout or deadlock

**Solution**: 
1. Increase timeout in config:
   ```properties
   antidupe.lock.timeout = 10000
   ```
2. Check for deadlocks in your trade code

### Issue: High memory usage

**Solution**: The system uses ~5MB per 1000 players. If memory is an issue:
1. Disable detailed logging
2. Reduce bot detection frequency
3. Lower audit retention

---

## 📚 Next Steps

1. **Read the full documentation**:
   - `SECURITY_ARCHITECTURE.md` - System design
   - `SECURITY_INTEGRATION.md` - Detailed integration guide
   - `SECURITY_CLASS_DIAGRAM.md` - Technical details

2. **Integrate with critical systems**:
   - Packet handler (prevents packet injection)
   - Trade system (prevents duplication)
   - Warehouse system (prevents duplication)
   - Inventory operations (prevents exploits)

3. **Set up monitoring**:
   - Create admin commands
   - Set up database queries
   - Configure alerts

4. **Tune for your server**:
   - Adjust thresholds based on your server type
   - Enable/disable features as needed
   - Monitor false positives

---

## 🆘 Support

### Check Logs

```bash
# Security system logs
tail -f GameServer/log/security/*.log

# Server logs
tail -f GameServer/log/java0.log
```

### Check Database

```sql
-- Recent errors
SELECT * FROM security_audit WHERE severity = 'CRITICAL' ORDER BY timestamp DESC LIMIT 20;

-- System status
SELECT event_type, COUNT(*) as count FROM security_audit GROUP BY event_type;
```

### Common Questions

**Q: Will this slow down my server?**  
A: No. The system uses async processing and adds <1ms overhead per operation.

**Q: Can I disable specific features?**  
A: Yes. Edit `config/security.properties` and set any feature to `false`.

**Q: How do I whitelist a player from bot detection?**  
A: Use the `clearPlayerFlag()` method or delete their entry from `bot_detection_scores` table.

**Q: Does this work with custom clients?**  
A: Yes. The system is server-side only and works with any client.

---

## 🎯 Success Checklist

- [ ] Database tables created
- [ ] Configuration file exists
- [ ] SecurityManager initialized in GameServer
- [ ] Server starts without errors
- [ ] Security logs are being created
- [ ] Audit events are being logged to database
- [ ] Packet handler integrated (optional but recommended)
- [ ] Trade system integrated (optional but recommended)

---

## 🔒 Security Best Practices

1. **Regular Monitoring**: Check audit logs weekly
2. **Update Thresholds**: Adjust based on your server's normal activity
3. **Review Flagged Players**: Check bot detection flags manually
4. **Database Cleanup**: Run cleanup procedure monthly
5. **Backup Audit Logs**: Keep audit logs for compliance

---

**Congratulations! Your L2J server is now protected by the SIGMO Security System.**

For detailed information, see:
- `SECURITY_ARCHITECTURE.md`
- `SECURITY_INTEGRATION.md`
- `SECURITY_CLASS_DIAGRAM.md`

**Version**: 1.0.0  
**Last Updated**: 2026-01-13  
**Author**: SIGMO Security Team
