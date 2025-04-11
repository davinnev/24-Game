import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// This class serves as the UI of the main game page
public class MainGame extends GameLobby {
    private JPanel gamePanel;
    private JPanel playersPanel;
    private CardLayout gameStateCards;
    private JPanel gameStatePanel;
    
    // Card names for the different game states
    private static final String JOINING_STATE = "JOINING";
    private static final String PLAYING_STATE = "PLAYING";
    private static final String FINISHED_STATE = "FINISHED";
    
    // The constructor follows superclass, then highlight the "Game" tab
    public MainGame(GamePlayer gamePlayer, Server gameServer) {
        super(gamePlayer, gameServer);
        highlightActiveTab(gameTab);
    }
    
    @Override
    protected void initializeContent() {
        bodyPanel.setLayout(new BorderLayout());
        bodyPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create game panel with card layout for different states
        gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // Create the card panel for different game states
        gameStateCards = new CardLayout();
        gameStatePanel = new JPanel(gameStateCards);
        
        // Add the three different game state panels
        gameStatePanel.add(createJoiningPanel(), JOINING_STATE);
        gameStatePanel.add(createPlayingPanel(), PLAYING_STATE);
        gameStatePanel.add(createFinishedPanel(), FINISHED_STATE);
        
        // Set default to joining state
        gameStateCards.show(gameStatePanel, JOINING_STATE);
        
        gamePanel.add(gameStatePanel, BorderLayout.CENTER);
        
        // Create player panel (right hand side)
        playersPanel = createPlayersPanel();

        bodyPanel.add(gamePanel, BorderLayout.CENTER);
        bodyPanel.add(playersPanel, BorderLayout.EAST);
    }
    
    // Method to change the game state
    public void setGameState(String state) {
        gameStateCards.show(gameStatePanel, state);
    }
    
    // Method to show joining state
    public void showJoiningState() {
        setGameState(JOINING_STATE);
    }
    
    // Method to show playing state
    public void showPlayingState() {
        setGameState(PLAYING_STATE);
    }
    
    // Method to show finished state
    public void showFinishedState(String winnerName) {
        // Update the winner label in the finished panel
        JLabel winnerLabel = (JLabel) ((JPanel) gameStatePanel.getComponent(2)).getComponent(1);
        winnerLabel.setText(winnerName + " is the winner!");
        setGameState(FINISHED_STATE);
    }
    
    // Panel for the joining stage
    private JPanel createJoiningPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout()); // Center content
        
        JButton joinButton = new JButton("Join New Game");
        joinButton.setFont(new Font("Arial", Font.BOLD, 16));
        joinButton.setPreferredSize(new Dimension(200, 60));
        
        // Add action listener to send join request to server
        joinButton.addActionListener(e -> {
            try {
                player.joinGame();
                joinButton.setEnabled(false);
                joinButton.setText("Waiting for players...");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to join game: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        panel.add(joinButton);
        return panel;
    }
    
    // Panel for the playing stage - current implementation
    private JPanel createPlayingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // This is where the actual game UI will go
        JLabel playingLabel = new JLabel("Game in progress...", JLabel.CENTER);
        playingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        panel.add(playingLabel, BorderLayout.CENTER);
        
        // Here you'll add your game elements later
        
        return panel;
    }
    
    // Panel for the finished stage
    private JPanel createFinishedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel gameOverLabel = new JLabel("Game Over!");
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gameOverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel winnerLabel = new JLabel("Winner will be displayed here");
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        winnerLabel.setForeground(new Color(0, 128, 0)); // Green color
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 16));
        playAgainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAgainButton.setMaximumSize(new Dimension(200, 60));
        
        // Add action listener to reset the game state
        playAgainButton.addActionListener(e -> {
            showJoiningState();
        });
        
        panel.add(Box.createVerticalGlue());
        panel.add(gameOverLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(winnerLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(playAgainButton);
        panel.add(Box.createVerticalGlue());
        
        return panel;
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
    
    // Method to update player list for the game
    public void updatePlayerList(String[] playerNames) {
        // Clear existing players except the current user
        playersPanel.removeAll();
        
        JLabel playersHeader = new JLabel("Players");
        playersHeader.setFont(new Font("Arial", Font.BOLD, 16));
        playersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        playersPanel.add(playersHeader);
        playersPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Add current user first
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
        
        playersPanel.add(currentUserPanel);
        playersPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Add other players
        for (String playerName : playerNames) {
            if (!playerName.equals(this.username)) {
                addPlayerEntry(playersPanel, playerName, "Win: 0/0 avg: 0.0s");
            }
        }
        
        playersPanel.revalidate();
        playersPanel.repaint();
    }
}