package com.akshaymathur.design.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database Schema Table name ConcurrentWrites
 * +----------+--------------+------+-----+---------+-------+
 * | Field | Type | Null | Key | Default | Extra |
 * +----------+--------------+------+-----+---------+-------+
 * | username | varchar(256) | NO | PRI | NULL | |
 * | counter | int | YES | | NULL | |
 * +----------+--------------+------+-----+---------+-------+
 */
public class ConcurrentWrites {
    private static final String USERNAME = "akshay";
    private static final String READ_USER = "SELECT * FROM ConcurrentWrites WHERE username = ?";
    // TRY REMOVING FOR UPDATE FROM THIS QUERY AND RUN THE DEMO AGAIN
    private static final String READ_COUNTER_FOR_USER = "Select counter From ConcurrentWrites WHERE username = ? FOR UPDATE";
    private static final String UPDATE_COUNTER_FOR_USER = "UPDATE ConcurrentWrites SET counter = ? WHERE username = ?";
    private static final String UPSERT_USER = "INSERT INTO ConcurrentWrites(username, counter) VALUES(?, ?) ON DUPLICATE KEY UPDATE counter=VALUES(counter)";
    private static final int CONCURRENCY_THREADS = 15;
    private static final String connectionUrl = "jdbc:mysql://localhost/DesignSystemDB";
    private ConcurrentHashMap<Long, Integer> threadCounterLog = new ConcurrentHashMap<>();

    public static Connection createConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            Connection conn = DriverManager.getConnection(connectionUrl, "root", "root");
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            return conn;
        } catch (SQLException e) {
            System.out.print(e);
            return null;
        }
    }

    private void upsertUser(String username) throws SQLException {
        try (Connection conn = createConnection();
                PreparedStatement upsertStatement = conn.prepareStatement(UPSERT_USER)) {
            upsertStatement.setString(1, username);
            // default counter is 0
            upsertStatement.setInt(2, 0);
            upsertStatement.executeUpdate();
            conn.commit();
        }
    }

    private void readAndUpdateCounterForUser(String username) throws SQLException {
        try (Connection conn = createConnection();
                PreparedStatement readStatement = conn.prepareStatement(READ_COUNTER_FOR_USER);
                PreparedStatement updateStatement = conn.prepareStatement(UPDATE_COUNTER_FOR_USER)) {
            readStatement.setString(1, username);
            ResultSet userCounterResult = readStatement.executeQuery();
            int counter = 0;
            while (userCounterResult.next()) {
                counter = userCounterResult.getInt("counter") + 1;
            }
            threadCounterLog.put(Thread.currentThread().getId(), counter);
            updateStatement.setInt(1, counter);
            updateStatement.setString(2, username);
            updateStatement.executeUpdate();
            conn.commit();
        }
    }

    private void printUserDetails(String username) throws SQLException {
        try (Connection conn = createConnection();
                PreparedStatement readStatement = conn.prepareStatement(READ_USER)) {
            readStatement.setString(1, username);
            ResultSet result = readStatement.executeQuery();
            while (result.next()) {
                System.out.println("--------------------");
                System.out.println("Username : " + result.getString(1));
                System.out.println("Counter : " + result.getInt(2));
            }
        }
    }

    private void printLog() {
        threadCounterLog.entrySet().stream()
                .forEach(e -> System.out.println("Thread Id: " + e.getKey() + " Value Seen: " + e.getValue()));
    }

    public void executeDemo() throws Exception {
        upsertUser(USERNAME);
        Thread[] threads = new Thread[CONCURRENCY_THREADS];
        for (int i = 0; i < CONCURRENCY_THREADS; i++) {
            threads[i] = new Thread() {
                public void run() {
                    try {
                        // UPSERT USER akshay and counter 1
                        readAndUpdateCounterForUser(USERNAME);
                    } catch (SQLException e) {
                        System.out.println("ERROR CODE: "+ e.getErrorCode());
                        System.out.println("SQL STATE: " + e.getSQLState());
                    }
                }
            };
        }
        Arrays.stream(threads).parallel().forEach(Thread::start);
        for (Thread t : threads) {
            t.join();
        }
        printUserDetails(USERNAME);
        printLog();
    }

    public static void main(String... s) {
        try {
            ConcurrentWrites demo = new ConcurrentWrites();
            demo.executeDemo();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}