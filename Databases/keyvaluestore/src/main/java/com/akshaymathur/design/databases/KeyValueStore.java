package com.akshaymathur.design.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * CREATE TABLE KeyValueStore (ukey VARCHAR(256) NOT NULL, uvalue TEXT,
 * expires_at TIMESTAMP, PRIMARY KEY(ukey));
 */
public class KeyValueStore {

    private static final String PUT_QUERY = "INSERT INTO KeyValueStore(ukey, uvalue, expires_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE uvalue=?, expires_at=? ;";
    private static final String GET_QUERY = "SELECT uvalue, expires_at FROM KeyValueStore WHERE ukey=?";

    private final Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/DesignSystemDB", "root", "root");
        conn.setAutoCommit(true);
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        return conn;
    }

    public void put(String key, String value) throws SQLException {
        // default expiry after 5 minutes.
        put(key, value, 300);
    }

    public void put(String key, String value, int expireAfterSeconds) throws SQLException {
        Instant expiry = Instant.now().plus(Duration.ofSeconds(expireAfterSeconds));
        try (Connection c = getConnection(); PreparedStatement putStatement = c.prepareStatement(PUT_QUERY)) {
            putStatement.setString(1, key);
            putStatement.setString(2, value);
            putStatement.setTimestamp(3, Timestamp.from(expiry));
            putStatement.setString(4, value);
            putStatement.setTimestamp(5, Timestamp.from(expiry));
            putStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public Optional<String> get(String key) throws SQLException {
        try (Connection c = getConnection();
                Statement getStatement = c.createStatement();
                ResultSet resultSet = getStatement.executeQuery(GET_QUERY.replace("?", "'" + key + "'"));) {
            if (resultSet.next()) {
                String value = resultSet.getString(1);
                Timestamp expires_at = resultSet.getTimestamp(2);
                if (Instant.now().isBefore(expires_at.toInstant())) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    public void tryGet() throws SQLException {
        Optional<String> value = get("akshay");
        if (value.isPresent()) {
            System.out.println("Key: akshay Value: " + value.get());
        } else {
            System.out.println("KEY 'akshay' NOT FOUND");
        }
    }

    public static void main(String[] args) throws SQLException, InterruptedException {
        int expireAfter = 10;
        KeyValueStore store = new KeyValueStore();
        store.put("akshay", "1");
        store.put("akshay", "1", expireAfter);
        store.tryGet();
        System.out.println("Sleeping for " + expireAfter + " seconds");
        Thread.sleep(Duration.ofSeconds(expireAfter + /* buffer */ 1).toMillis());
        store.tryGet();
    }
}