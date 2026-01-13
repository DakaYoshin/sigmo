# L2jSigmo
Free project Interlude

1.  **Driver Updated**:
    *   `mariadb-java-client-3.3.3.jar` is in `f:\sigmo\GameServer\lib\`.

2.  **Configuration**:
    *   `build.xml`, `.properties` files, and IDE `.classpath` are updated.

3.  **Code Adjustments**:
    *   `FileLogFormatter.java`: 
        *   Renamed variable `_` to `TAB` (Java 9+ compliance).
        *   Replaced deprecated `getThreadID()` with `getLongThreadID()` (Java 16+ compliance).
           *   `AdminAnnouncements.java`:
        *   Replaced deprecated `new Integer(String)` with `Integer.parseInt(String)`.
            *   `AdminLogin.java`:
        *   Replaced deprecated `new Integer(String)` with `Integer.parseInt(String)`.
     *   `GameServerTable.java`:
        *   Replaced deprecated `new Integer(String)` with `Integer.parseInt(String)`.       
    *   `ClanTable.java`:
        *   Replaced deprecated `new Integer(int)` with `Integer.valueOf(int)`.
    *   `ClanTable.java`:
        *   Replaced deprecated `new Integer(int)` with `Integer.valueOf(int)`.
4.  **Scripts Updated**:
    *   All `.bat` files updated for Java 17 flags.
    
5.  **NEXT STEPS**:
    1.  **Install JDK 17**: Ensure you have Java 17 installed on your system.
    2.  **Install MariaDB**: Ensure you have MariaDB 10+ installed and running.
    3.  **Compile**: Open a terminal in `f:\sigmo\GameServer` and run `ant dist`.
    
Your project is clean and ready to build!