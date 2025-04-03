import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// This class serves as the client as a player in the game
public class GamePlayer implements Runnable  {

    private Server gameServer;
    private JFrame frame;
    private PlayerLogin loginPanel;
    private PlayerRegister registerPanel;
    private Profile profilePanel;
    private Leaderboard leaderboardPanel;
    private MainGame gamePanel;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private String username;

    // Getter function for username
    public String getUsername() {
        return username;
    }

    // Setter function for username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter function for frame
    public JFrame getFrame() {
        return frame;
    }

    // Function to set frame size
    public void setFrameSize(int width, int height) {
        frame.setSize(width, height);
    }

    // This function helps with connecting with the RMI registry service
    public GamePlayer() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            this.gameServer = (Server)registry.lookup("GameServer");
            System.out.println("Server found" + this.gameServer);
        } catch (Exception e){
            System.err.println("Error accessing RMI: " + e.toString());
        }
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(new GamePlayer());
    }

    // The "show.." functions below help to transition between different UI pages

    public void showLogin() {
        cardLayout.show(cardPanel, "login");
        frame.setTitle("Login");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        loginPanel.clearText();
        loginPanel.clearError();
    }

    public void showRegister() {
        cardLayout.show(cardPanel, "register");
        frame.setTitle("Register");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        registerPanel.clearText();
        registerPanel.clearError();
    }

    public void showProfile() {
        if (profilePanel == null) {
            profilePanel = new Profile(this, gameServer);
            cardPanel.add(profilePanel, "profile");
        }
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);
        profilePanel.highlightActiveTab(profilePanel.profileTab);
        cardLayout.show(cardPanel, "profile");
    }

    public void showLeaderboard() {
        if (leaderboardPanel == null) {
            leaderboardPanel = new Leaderboard(this, gameServer);
            cardPanel.add(leaderboardPanel, "leaderboard");
        }
        frame.setSize(1000, 600);
        leaderboardPanel.highlightActiveTab(leaderboardPanel.leaderboardTab);
        cardLayout.show(cardPanel, "leaderboard");
    }

    public void showMainGame() {
        if (gamePanel == null) {
            gamePanel = new MainGame(this, gameServer);
            cardPanel.add(gamePanel, "game");
        }
        frame.setSize(1000, 600);
        gamePanel.highlightActiveTab(gamePanel.gameTab);
        cardLayout.show(cardPanel, "game");
    }
    
    // This function helps initiating the UI on the auth page
    public void setupFrame(){

        // Main frame of the game
        frame = new JFrame("24-Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.getContentPane().setBackground(new Color(220, 255, 220));

        // CardLayout to switch between login and register
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        frame.add(cardPanel);
        frame.setLocationRelativeTo(null);
    }

    public void drawAuthPanel(){
        loginPanel = new PlayerLogin(this, gameServer);
        registerPanel = new PlayerRegister(this, gameServer);
        cardPanel.add(loginPanel, "login");
        cardPanel.add(registerPanel, "register");
        cardLayout.show(cardPanel, "login");
    }

    public void run() {
        setupFrame();
        drawAuthPanel();
        frame.setVisible(true);
    }

    // We call this function every time a user logouts
    public void reset() {
        this.setUsername(null);

        if (profilePanel != null) {
            cardPanel.remove(profilePanel);
            profilePanel = null;
        }
        if (leaderboardPanel != null) {
            cardPanel.remove(leaderboardPanel);
            leaderboardPanel = null;
        }
        if (gamePanel != null) {
            cardPanel.remove(gamePanel);
            gamePanel = null;
        }
    }
}