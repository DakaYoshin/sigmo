/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.util.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.Config;

public class L2DatabaseFactory {
	private static final Logger _log = LoggerFactory.getLogger(L2DatabaseFactory.class);

	public static enum ProviderType {
		MySql,
		MsSql
	}

	private static L2DatabaseFactory _instance;
	private ProviderType _providerType;
	private HikariDataSource _source;

	public L2DatabaseFactory() throws SQLException {
		try {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(Config.DATABASE_URL);
			config.setUsername(Config.DATABASE_LOGIN);
			config.setPassword(Config.DATABASE_PASSWORD);
			config.setDriverClassName(Config.DATABASE_DRIVER);

			// Pooling Settings
			config.setMaximumPoolSize(Math.max(10, Config.DATABASE_MAX_CONNECTIONS));
			config.setMinimumIdle(10);
			config.setIdleTimeout(60000);
			config.setConnectionTimeout(Config.DATABASE_TIMEOUT > 0 ? Config.DATABASE_TIMEOUT : 30000);

			// Performance optimizations
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

			_source = new HikariDataSource(config);

			if (Config.DATABASE_DRIVER.toLowerCase().contains("microsoft")) {
				_providerType = ProviderType.MsSql;
			} else {
				_providerType = ProviderType.MySql;
			}
		} catch (Exception e) {
			// Hikari throws RuntimeException mostly, but we wrap/rethrow as SQLException
			// for compatibility
			throw new SQLException("could not init DB connection", e);
		}
	}

	public final String prepQuerySelect(String[] fields, String tableName, String whereClause,
			boolean returnOnlyTopRecord) {
		String msSqlTop1 = "";
		String mySqlTop1 = "";
		if (returnOnlyTopRecord) {
			if (getProviderType() == ProviderType.MsSql) {
				msSqlTop1 = " Top 1 ";
			}

			if (getProviderType() == ProviderType.MySql) {
				mySqlTop1 = " Limit 1 ";
			}
		}
		String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause
				+ mySqlTop1;
		return query;
	}

	public static void close(Connection con) {
		if (con == null)
			return;

		try {
			con.close();
		} catch (SQLException e) {
			_log.error("Failed to close database connection! " + e);
		}
	}

	public void shutdown() {
		try {
			_source.close();
		} catch (Exception e) {
			_log.error("", e);
		}

		try {
			_source = null;
		} catch (Exception e) {
			_log.error("", e);
		}
	}

	public final String safetyString(String[] whatToCheck) {
		String braceLeft = "`";
		String braceRight = "`";

		if (getProviderType() == ProviderType.MsSql) {
			braceLeft = "[";
			braceRight = "]";
		}

		String result = "";

		for (String word : whatToCheck) {
			if (result != "") {
				result += ", ";
			}
			result += braceLeft + word + braceRight;
		}
		return result;
	}

	public static L2DatabaseFactory getInstance() throws SQLException {
		if (_instance == null) {
			_instance = new L2DatabaseFactory();
		}
		return _instance;
	}

	public Connection getConnection() throws SQLException {
		Connection con = null;

		while (con == null) {
			try {
				con = _source.getConnection();
			} catch (SQLException e) {
				_log.error("L2DatabaseFactory: geting connection failed, trying again", e);
			}
		}

		return con;
	}

	public int getBusyConnectionCount() throws SQLException {
		// Not supported directly in HikariCP without JMX/MXBean usage.
		// Returning -1 to indicate unavailable stat.
		return -1;
		// return _source.getNumBusyConnectionsDefaultUser();
	}

	public int getIdleConnectionCount() throws SQLException {
		// Not supported directly in HikariCP without JMX/MXBean usage.
		return -1;
		// return _source.getNumIdleConnectionsDefaultUser();
	}

	public final ProviderType getProviderType() {
		return _providerType;
	}

}