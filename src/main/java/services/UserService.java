package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;

import models.User;

public class UserService {
    private static volatile UserService instance = null; // Volatile for thread safety
    private static final Dotenv dotenv = Dotenv.load();

    // PostgreSQL Database Connection Configuration
    private static final String DB_URL = dotenv.get("DB_URL");
    private static final String DB_USER = dotenv.get("DB_USER");
    private static final String DB_PASSWORD = dotenv.get("DB_PASSWORD");

    private static final String USERS_TABLE_CREATE_QUERY =
            "CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(255) PRIMARY KEY, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "is_logged_in BOOLEAN DEFAULT FALSE" +
                    ")";

    private static final String CONTACTS_TABLE_CREATE_QUERY =
            "CREATE TABLE IF NOT EXISTS contacts (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "phone_number VARCHAR(20) NOT NULL, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE" +
                    ")";

    // Private constructor to enforce Singleton
    private UserService() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            // Create users table
            stmt.execute(USERS_TABLE_CREATE_QUERY);

            // Create contacts table
            stmt.execute(CONTACTS_TABLE_CREATE_QUERY);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing the database.");
        }
    }

    // Thread-safe Singleton method
    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) { // Synchronize to ensure only one instance is created
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    public boolean userExists(String username) {
        String query = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet resultSet = pstmt.executeQuery();
            return resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean validateUser(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                return storedPassword.equals(password);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addUser(String username, String password) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUser(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                String password = resultSet.getString("password");
                boolean isLoggedIn = resultSet.getBoolean("is_logged_in");
                User user = new User(username, password);
                user.setLoggedIn(isLoggedIn);
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean login(String username, String password) {
        if (validateUser(username, password)) {
            String query = "UPDATE users SET is_logged_in = TRUE WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, username);
                pstmt.executeUpdate();
                return true;

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void logout(String username) {
        String query = "UPDATE users SET is_logged_in = FALSE WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addContact(String username, String name, String phoneNumber) {
        String query = "INSERT INTO contacts (name, phone_number, username) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, name);
            pstmt.setString(2, phoneNumber);
            pstmt.setString(3, username);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getContacts(String username) {
        String query = "SELECT name, phone_number FROM contacts WHERE username = ?";
        List<String> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String contact = rs.getString("name") + " - " + rs.getString("phone_number");
                contacts.add(contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public boolean removeContactByName(String username, String name) {
        String query = "DELETE FROM contacts WHERE username =? AND name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, name);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Returns true if a row was deleted

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> searchContactsByName(String username, String name) {
        String query = "SELECT name, phone_number FROM contacts WHERE username = ? AND name ILIKE ?";
        List<String> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, "%" + name + "%"); // Use wildcards for partial matching
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String contact = rs.getString("name") + " - " + rs.getString("phone_number");
                contacts.add(contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public boolean updateContactByName(String username, String oldName, String newName, String newPhoneNumber) {
        String query = "UPDATE contacts SET name = ?, phone_number = ? WHERE username = ? AND name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newName);       // New name
            pstmt.setString(2, newPhoneNumber); // New phone number
            pstmt.setString(3, username);      // Username
            pstmt.setString(4, oldName);       // Old name

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Returns true if a row was updated

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



}
