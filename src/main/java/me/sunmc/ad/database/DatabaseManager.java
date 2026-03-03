package me.sunmc.ad.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.sunmc.ad.AgriDeco;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public final class DatabaseManager {

    private final AgriDeco plugin;
    private HikariDataSource dataSource;
    private StorageType storageType;
    public DatabaseManager(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @Contract(" -> new")
    public @NotNull CompletableFuture<Void> initAsync() {
        return CompletableFuture.runAsync(() -> {
            var cfg = plugin.getConfig();
            var type = cfg.getString("storage-type", "sqlite").trim().toLowerCase();
            storageType = type.equals("mysql") ? StorageType.MYSQL : StorageType.SQLITE;

            var hk = new HikariConfig();
            hk.setPoolName("AgriDeco-Pool");

            if (storageType == StorageType.SQLITE) {
                try {
                    plugin.getDataFolder().mkdirs();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                var dbFile = new File(plugin.getDataFolder(), "data.db");
                hk.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hk.setDriverClassName("org.sqlite.JDBC");
                hk.setMaximumPoolSize(1);
                hk.setMinimumIdle(1);
                hk.setConnectionTestQuery("SELECT 1");
                hk.setConnectionInitSql(
                        "PRAGMA journal_mode=WAL; " +
                                "PRAGMA synchronous=NORMAL; " +
                                "PRAGMA foreign_keys=ON;");
            } else {
                hk.setJdbcUrl(String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        cfg.getString("database.host", "localhost"),
                        cfg.getInt("database.port", 3306),
                        cfg.getString("database.database", "agrideco")));
                hk.setUsername(cfg.getString("database.username", "root"));
                hk.setPassword(cfg.getString("database.password", "password"));
                hk.setDriverClassName("com.mysql.cj.jdbc.Driver");
                hk.setMaximumPoolSize(cfg.getInt("database.pool.maximum_pool_size", 10));
                hk.setMinimumIdle(cfg.getInt("database.pool.minimum_idle", 2));
                hk.setConnectionTimeout(cfg.getLong("database.pool.connection_timeout", 30000));
                hk.setIdleTimeout(cfg.getLong("database.pool.idle_timeout", 600000));
                hk.setMaxLifetime(cfg.getLong("database.pool.max_lifetime", 1800000));
                hk.addDataSourceProperty("cachePrepStmts", "true");
                hk.addDataSourceProperty("prepStmtCacheSize", "250");
                hk.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                hk.addDataSourceProperty("useServerPrepStmts", "true");
                hk.addDataSourceProperty("useLocalSessionState", "true");
                hk.addDataSourceProperty("rewriteBatchedStatements", "true");
                hk.addDataSourceProperty("cacheResultSetMetadata", "true");
                hk.addDataSourceProperty("cacheServerConfiguration", "true");
                hk.addDataSourceProperty("elideSetAutoCommits", "true");
                hk.addDataSourceProperty("maintainTimeStats", "false");
            }

            dataSource = new HikariDataSource(hk);
            createTables();
            plugin.getSLF4JLogger().info("Database ready ({}).", storageType);
        });
    }

    private void createTables() {
        try (var c = dataSource.getConnection(); var st = c.createStatement()) {
            if (storageType == StorageType.SQLITE) {
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS agrideco_furniture (
                            uuid       TEXT    NOT NULL PRIMARY KEY,
                            config_id  TEXT    NOT NULL,
                            world      TEXT    NOT NULL,
                            x          REAL    NOT NULL,
                            y          REAL    NOT NULL,
                            z          REAL    NOT NULL,
                            yaw        REAL    NOT NULL DEFAULT 0,
                            owner      TEXT    NOT NULL,
                            state      INTEGER NOT NULL DEFAULT 0,
                            placed_at  TEXT    NOT NULL DEFAULT (datetime('now'))
                        )""");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS agrideco_crops (
                            uuid       TEXT    NOT NULL PRIMARY KEY,
                            config_id  TEXT    NOT NULL,
                            world      TEXT    NOT NULL,
                            x          REAL    NOT NULL,
                            y          REAL    NOT NULL,
                            z          REAL    NOT NULL,
                            stage      INTEGER NOT NULL DEFAULT 0,
                            owner      TEXT    NOT NULL,
                            planted_at TEXT    NOT NULL DEFAULT (datetime('now'))
                        )""");
            } else {
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS agrideco_furniture (
                            uuid       CHAR(36)    NOT NULL PRIMARY KEY,
                            config_id  VARCHAR(64) NOT NULL,
                            world      VARCHAR(64) NOT NULL,
                            x          DOUBLE      NOT NULL,
                            y          DOUBLE      NOT NULL,
                            z          DOUBLE      NOT NULL,
                            yaw        FLOAT       NOT NULL DEFAULT 0,
                            owner      CHAR(36)    NOT NULL,
                            state      INT         NOT NULL DEFAULT 0,
                            placed_at  TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS agrideco_crops (
                            uuid       CHAR(36)    NOT NULL PRIMARY KEY,
                            config_id  VARCHAR(64) NOT NULL,
                            world      VARCHAR(64) NOT NULL,
                            x          DOUBLE      NOT NULL,
                            y          DOUBLE      NOT NULL,
                            z          DOUBLE      NOT NULL,
                            stage      INT         NOT NULL DEFAULT 0,
                            owner      CHAR(36)    NOT NULL,
                            planted_at TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Table creation failed", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed())
            throw new SQLException("DataSource unavailable.");
        return dataSource.getConnection();
    }

    public boolean isAvailable() {
        return dataSource != null && !dataSource.isClosed();
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void shutdown() {
        if (isAvailable()) dataSource.close();
    }

    public enum StorageType {SQLITE, MYSQL}
}