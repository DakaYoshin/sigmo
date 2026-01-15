# Project Sigmo - Interlude Server

Free project Interlude based on L2J, modernized and enhanced for performance and features.

## 🚀 Recent Accomplishments

### 1. Modernization & Infrastructure
*   **Java 17 Migration**: Full codebase upgrade to Java 17. Fixed syntax compliance issues (such as `_` identifier usage), updated deprecated constructors (e.g., `new Integer()`), and optimized JVM flags in all `.bat` scripts.
*   **SLF4J Implementation**: Migrated the entire project from legacy `java.util.logging` to **SLF4J**, providing a more flexible and high-performance logging framework.
*   **Project Consolidation**: Streamlined the development environment by merging the `DataPack` scripts and handlers into the `GameServer` project. This reduced pathing complexity and improved build times.
*   **Dependency Updates**: Updated core libraries including `mariadb-java-client` to version 3.3.3.

### 2. New Features & Enhancements
*   **Advanced Auto Farm System**:
    *   Implemented a fully configurable Auto Farm system for players.
    *   Features include: radius control, HP/MP potion management, skill usage percentages, and ignored monster lists.
    *   Integrated persistent database storage for player preferences.
    *   New voiced command `.autofarm` and GUI menus.
*   **Refactored Handler System**: Implemented `MasterHandler` to centralize the registration of Admin, Voiced, User, Chat, and Item handlers, significantly cleaning up the `GameServer` initialization logic.

### 3. Stability & Security
*   **Admin Command Fixes**: Resolved issues where admin commands were not being correctly registered or were inaccessible due to package path errors.
*   **Anti-Dupe & Security**: Fixed compilation and logic errors in the `AntiDupeGuard` system.
*   **Deadlock Prevention**: Refactored the `DeadlockDetector` daemon for better compatibility with modern thread management.
*   **Script Engine Stability**: Fixed various compilation issues within the `L2ScriptEngineManager` and ensured proper script compilation caching.

## 🛠️ Requirements & Setup

1.  **Java Development Kit (JDK) 17**: Ensure your `JAVA_HOME` points to a JDK 17 installation.
2.  **MariaDB 10+**: Database server required for character and server data.
3.  **Build Tool**: Apache Ant is used for compilation.

### How to Compile
1.  Open a terminal in the `f:\sigmo\GameServer` directory.
2.  Run the command: `ant dist`.
3.  The compiled server files will be located in the `build/dist` folder.

### Database Setup
1.  Create a new schema in MariaDB.
2.  Execute the updated SQL scripts located in the `DataPack/sql` folder (if applicable).
3.  Configure `config/server.properties` with your database credentials.

---
*Your project is clean, modernized, and ready for production testing!*