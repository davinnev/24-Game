import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// This class serves as the UI of the main game page
public class MainGame extends GameLobby {
    private JPanel gamePanel;
    private JPanel playersPanel;
    
    // The constructor follows superclass, then highlight the "Game" tab
    public MainGame(GamePlayer gamePlayer, Server gameServer) {
        super(gamePlayer, gameServer);
        highlightActiveTab(gameTab);
    }
    
    @Override
    protected void initializeContent() {
        bodyPanel.setLayout(new BorderLayout());
        bodyPanel.setBorder(new EmptyBorder(20,20,20,20));
        
        // Create placeholder for game (to be implemented later)
        gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel placeholderLabel = new JLabel("To be implemented", JLabel.CENTER);
        placeholderLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        gamePanel.add(placeholderLabel, BorderLayout.CENTER);
        
        // Create player panel (right hand side)
        playersPanel = createPlayersPanel();

        bodyPanel.add(gamePanel, BorderLayout.CENTER);
        bodyPanel.add(playersPanel, BorderLayout.EAST);
    }
    
    // This function creates the 4 players panel on the right hand side
    private JPanel createPlayersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(200, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        panel.setBackground(new Color(240, 240, 240));
        
        JLabel playersHeader = new JLabel("Players");
        playersHeader.setFont(new Font("Arial", Font.BOLD, 16));
        playersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Current user
        JPanel currentUserPanel = new JPanel();
        currentUserPanel.setLayout(new BoxLayout(currentUserPanel, BoxLayout.Y_AXIS));
        currentUserPanel.setBackground(new Color(230, 255, 230));
        currentUserPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        currentUserPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        currentUserPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel nameLabel = new JLabel(this.username);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel statsLabel = new JLabel("Win: 20/35 avg: 10.4s");
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        currentUserPanel.add(Box.createVerticalGlue());
        currentUserPanel.add(nameLabel);
        currentUserPanel.add(statsLabel);
        currentUserPanel.add(Box.createVerticalGlue());
        
        panel.add(playersHeader);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(currentUserPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Other players (dummy data)
        addPlayerEntry(panel, "Some player", "Win: 20/35 avg: 10.4s");
        addPlayerEntry(panel, "Someone", "Win: 20/35 avg: 10.4s");
        addPlayerEntry(panel, "Somebody", "Win: 20/35 avg: 10.4s");
        
        return panel;
    }
    
    // This function creates the other player's profile on the main game screen
    private void addPlayerEntry(JPanel container, String name, String stats) {
        JPanel playerPanel = new JPanel();
        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
        playerPanel.setBackground(new Color(245, 245, 245));
        playerPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        playerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        playerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel statsLabel = new JLabel(stats);
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        playerPanel.add(Box.createVerticalGlue());
        playerPanel.add(nameLabel);
        playerPanel.add(statsLabel);
        playerPanel.add(Box.createVerticalGlue());
        
        container.add(playerPanel);
        container.add(Box.createRigidArea(new Dimension(0, 5)));
    }
}