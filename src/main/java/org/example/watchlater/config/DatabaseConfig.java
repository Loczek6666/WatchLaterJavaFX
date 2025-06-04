package org.example.watchlater.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

@Slf4j
@Data
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private HikariDataSource dataSource;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private int maximumPoolSize;
    private int minimumIdle;
    private int idleTimeout;
    private int connectionTimeout;
    private int maxLifetime;
    private boolean autoCommit;
    private String connectionTestQuery;
    
    private DatabaseConfig() {
        Properties envProps = new Properties();
        try {
            envProps.load(new FileInputStream(".env"));
            
            String dbUrl = envProps.getProperty("DB_URL");
            String dbUsername = envProps.getProperty("DB_USERNAME");
            String dbPassword = envProps.getProperty("DB_PASSWORD");
            
            if (dbUrl == null || dbUsername == null || dbPassword == null) {
                throw new RuntimeException("Missing required database configuration in .env file");
            }

            String[] urlParts = dbUrl.replace("postgresql://", "").split("/");
            if (urlParts.length != 2) {
                throw new RuntimeException("Invalid database URL format in .env file");
            }
            
            String[] hostPort = urlParts[0].split(":");
            if (hostPort.length != 2) {
                throw new RuntimeException("Invalid host:port format in database URL");
            }
            
            String host = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);
            String database = urlParts[1];
            
            Properties props = new Properties();
            props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
            props.setProperty("dataSource.user", dbUsername);
            props.setProperty("dataSource.password", dbPassword);
            props.setProperty("dataSource.databaseName", database);
            props.setProperty("dataSource.portNumber", String.valueOf(port));
            props.setProperty("dataSource.serverName", host);
            
            HikariConfig config = new HikariConfig(props);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(20000);
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            
            this.dataSource = new HikariDataSource(config);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration from .env file", e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid port number in database URL", e);
        }
    }
    
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    public void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        config.setMaximumPoolSize(maximumPoolSize > 0 ? maximumPoolSize : 10);
        config.setMinimumIdle(minimumIdle > 0 ? minimumIdle : 5);
        config.setIdleTimeout(idleTimeout > 0 ? idleTimeout : 300000);
        config.setConnectionTimeout(connectionTimeout > 0 ? connectionTimeout : 30000);
        config.setMaxLifetime(maxLifetime > 0 ? maxLifetime : 1800000);
        config.setAutoCommit(autoCommit);
             
        Properties props = new Properties();
        props.setProperty("cachePrepStmts", "true");
        props.setProperty("prepStmtCacheSize", "250");
        props.setProperty("prepStmtCacheSqlLimit", "2048");
        props.setProperty("useServerPrepStmts", "true");
        config.setDataSourceProperties(props);
        
        try {
            if (dataSource != null) {
                dataSource.close();
            }
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool closed");
        }
    }
    
    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGSERIAL PRIMARY KEY,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    display_name VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP WITH TIME ZONE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS movie_cache (
                    id BIGSERIAL PRIMARY KEY,
                    tmdb_id INTEGER UNIQUE NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    overview TEXT,
                    poster_path VARCHAR(255),
                    backdrop_path VARCHAR(255),
                    release_date DATE,
                    vote_average DOUBLE PRECISION,
                    vote_count INTEGER,
                    genres INTEGER[],
                    original_language VARCHAR(10),
                    original_title VARCHAR(255),
                    popularity DOUBLE PRECISION,
                    adult BOOLEAN,
                    video BOOLEAN,
                    cache_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS watchlist (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT REFERENCES users(id),
                    movie_id BIGINT REFERENCES movie_cache(id),
                    watched BOOLEAN DEFAULT FALSE,
                    priority INTEGER DEFAULT 0,
                    notes TEXT,
                    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    watched_at TIMESTAMP WITH TIME ZONE,
                    UNIQUE(user_id, movie_id)
                )
            """);

            stmt.execute("""
                INSERT INTO users (id, email, display_name)
                VALUES (1, 'default@watchlater.com', 'Default User')
                ON CONFLICT (id) DO NOTHING
            """);
            
        } catch (SQLException e) {
            log.error("Error initializing database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
} 