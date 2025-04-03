import java.rmi.*;
import java.rmi.server.*;
import java.sql.*;

// This class serves as the game server that handles client's requests
public class GameServer extends UnicastRemoteObject implements Server{
    private static final String DB_HOST = "jdbc:mysql://localhost:3306/c3358";
	private static final String DB_USER = "c3358";
	private static final String DB_PASS = "c3358PASS";
    private Connection dbConn;

    public GameServer() throws RemoteException {
        // Connect to DB 
        try {
            this.dbConn = DriverManager.getConnection(DB_HOST, DB_USER, DB_PASS);
            System.out.println("Database connected!");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    // synchronized is used to retain consistency across server

    // This function handles login request from the client
    public synchronized boolean login(String username, String password) throws RemoteException {
        //System.out.println("Login request for: " + username);

        boolean isPasswordCorrect = false;
        boolean isNotLoggedIn = false;

        // Read a row from user_info, matching username and password
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT password FROM user_info WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                if (dbPassword.equals(password)) {
                    isPasswordCorrect = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Read a row from online_user, matching if username exists
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT username FROM online_user WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                isNotLoggedIn = true;
            }
        }   catch (SQLException e) {
            e.printStackTrace();
        }

        // If password is correct and user is not logged in, add to online_user
        if (isPasswordCorrect && isNotLoggedIn) {
            try {
                PreparedStatement stmt = this.dbConn.prepareStatement("INSERT INTO online_user (username) VALUES (?)");
                stmt.setString(1, username);
                stmt.execute();
                //System.out.println("Users added to ONLINE USERS");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //System.out.println("Logged in as " + username);
            return true;
        }
        return false;
    }

    // This function handles register (sign up) request from the client
    public synchronized boolean register(String username, String password) throws RemoteException {
        //System.out.println("Registration request for: " + username);

        // Check if username alread exists in user_info to avoid duplicate username
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT username FROM user_info WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Insert into user_info
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("INSERT INTO user_info (username, password) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.execute();
            System.out.println("User added to USER INFO");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Insert into online_user
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("INSERT INTO online_user (username) VALUES (?)");
            stmt.setString(1, username);
            stmt.execute();
            System.out.println("User added to ONLINE USERS");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        //System.out.println("Registered and logged in as " + username);
        return true;
    }

    // This function handles logout request from the client
    public synchronized boolean logout(String username) throws RemoteException {
        //System.out.println("Logout request for: " + username);

        // Update online_user. Remove the username from the list
        // Assumption: if there is a request from a username that does not even exist in the table initially, it is still considered a successful logout
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("DELETE FROM online_user WHERE username = ?");
            stmt.setString(1, username);
            stmt.execute();
            System.out.println("Users removed from ONLINE USERS");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public static void main(String[] args) {
        System.out.println("Server started");
        try {
            GameServer app = new GameServer();
            System.setSecurityManager(new SecurityManager());
            Naming.rebind("GameServer", app);
            System.out.println("Service registered");
        } catch (Exception e) {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
