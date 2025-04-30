import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

// This class serves as the UI of the profile page
public class Profile extends GameLobby {

    // UI components for profile page data
    private JLabel usernameLabel;
    private JLabel winsLabel;
    private JLabel gamesLabel;
    private JLabel avgTimeLabel;
    private JLabel rankLabel;

    // The constructor follows superclass, then highlight the "Profile" tab
    public Profile(GamePlayer gamePlayer, Server gameServer) {
        super(gamePlayer, gameServer);
        highlightActiveTab(profileTab);
    }

    // This function retrieves the user data from the database
    private ResultSet retrieveUserData() {
        try {
            String query = "SELECT * FROM user_info WHERE username = ?";
            PreparedStatement statement = super.getDBConn().prepareStatement(query);
            statement.setString(1,super.getUsername());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user data: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void initializeContent() {
        int gamesWon = 0;
        int gamesPlayed = 0;
        float avgWinTime = 0;
        int rank = 0;

        ResultSet resultSet = retrieveUserData();

        try {
            gamesWon = resultSet.getInt("games_won");
            gamesPlayed = resultSet.getInt("games_played");
            avgWinTime = resultSet.getFloat("avg_win_time");
            rank = resultSet.getInt("rank");
        } catch (SQLException e) {
            System.err.println("Error retrieving user data: " + e.getMessage());
        }

        bodyPanel.setLayout(new BorderLayout());
        bodyPanel.setBorder(new EmptyBorder(20,20,20,20));

        // Main panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(10,10,10,10));

        // Username section
        usernameLabel = new JLabel(this.username);
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Player stats 
        winsLabel = new JLabel("Number of wins: " + gamesWon);
        winsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        winsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        winsLabel.setBorder(new EmptyBorder(10, 0, 5, 0));
        
        gamesLabel = new JLabel("Number of games: " + gamesPlayed);
        gamesLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gamesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gamesLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        
        avgTimeLabel = new JLabel("Average time to win: " + avgWinTime + "s");
        avgTimeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        avgTimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        avgTimeLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        rankLabel = new JLabel("Rank: " + rank);
        rankLabel.setFont(new Font("Arial", Font.BOLD, 20));
        rankLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoPanel.add(usernameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        infoPanel.add(winsLabel);
        infoPanel.add(gamesLabel);
        infoPanel.add(avgTimeLabel);
        infoPanel.add(rankLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        bodyPanel.add(infoPanel, BorderLayout.NORTH);
        
    }

    // This function helps to refresh the profile page
    public void refreshProfile() {
        // Reset and reinitialize the existing content
        bodyPanel.removeAll();
        initializeContent();
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }
}