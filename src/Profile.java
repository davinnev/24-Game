import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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
    
    @Override
    protected void initializeContent() {
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
        winsLabel = new JLabel("Number of wins: 10");
        winsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        winsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        winsLabel.setBorder(new EmptyBorder(10, 0, 5, 0));
        
        gamesLabel = new JLabel("Number of games: 20");
        gamesLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gamesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gamesLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        
        avgTimeLabel = new JLabel("Average time to win: 12.5s");
        avgTimeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        avgTimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        avgTimeLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        rankLabel = new JLabel("Rank: #10");
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
}