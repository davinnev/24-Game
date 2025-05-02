import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

// This class serves as the UI of the main game page
public class MainGame extends GameLobby {
    private JPanel gamePanel;
    private JPanel playersPanel;
    private JPanel cardsPanel;
    private JTextField equationField;
    private CardLayout gameStateCards;
    private JPanel gameStatePanel;
    private String state;
    private String message;
    
    // Different possible game states
    private static final String JOINING_STATE = "JOINING";
    private static final String PLAYING_STATE = "PLAYING";
    private static final String FINISHED_STATE = "FINISHED";
    
    // The constructor follows superclass, then highlight the "Game" tab
    public MainGame(JPoker24Game gamePlayer, Server gameServer) {
        super(gamePlayer, gameServer);
        highlightActiveTab(gameTab);
    }

    // This method sets the message to be displayed in the finished state
    public void setMessage(String message) {
        this.message = message;
    }

    // This method gets the message to be displayed in the finished state
    public String getMessage() {
        return this.message;
    }
    
    // This method initializes content of the game tab (placeholder)
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
        this.state = JOINING_STATE;
        gameStateCards.show(gameStatePanel, JOINING_STATE);
        
        gamePanel.add(gameStatePanel, BorderLayout.CENTER);
        
        // Create player panel (right hand side)
        updatePlayersPanel("this.username" + "0/0 0.0"); 
        rebuildPlayersPanel();
        
        bodyPanel.add(gamePanel, BorderLayout.CENTER);
        bodyPanel.add(playersPanel, BorderLayout.EAST);
    }
    
    // This method changes the game state
    public void setGameState(String state) {
        // If finishes, refresh the info by showing the win information
        if (state.equals(FINISHED_STATE)) {
            for (Component comp : gameStatePanel.getComponents()) {
                if (comp.getName() != null && comp.getName().equals(FINISHED_STATE)) {
                    gameStatePanel.remove(comp);
                    break;
                }
            }
        
            // Create and add the new panel
            JPanel finishedPanel = createFinishedPanel();
            finishedPanel.setName(FINISHED_STATE);
            gameStatePanel.add(finishedPanel, FINISHED_STATE);
            
            gameStatePanel.revalidate();
            gameStatePanel.repaint();
        }
        
        gameStateCards.show(gameStatePanel, state);
        this.state = state;
        
        // If not a playing state, no need to show players panel
        if (this.state.equals(PLAYING_STATE)) {
            bodyPanel.add(playersPanel, BorderLayout.EAST);
        } else {
            bodyPanel.remove(playersPanel);
        }
        
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // This method gets the current game state
    public String getGameState() {
        return this.state;
    }
    
    // This method shows joining state
    public void showJoiningState() {
        setGameState(JOINING_STATE);
    }
    
    // This method shows playing state
    public void showPlayingState() {
        gameStatePanel.remove(1);
        gameStatePanel.add(createPlayingPanel(), PLAYING_STATE);
        setGameState(PLAYING_STATE);
    }
    
    // This method shows finished state
    public void showFinishedState(String winnerName) {
        // Update the winner label in the finished panel
        JLabel winnerLabel = (JLabel) ((JPanel) gameStatePanel.getComponent(2)).getComponent(1);
        winnerLabel.setText(winnerName + " is the winner!");
        setGameState(FINISHED_STATE);
    }
    
    // This method creates the joining stage interface
    private JPanel createJoiningPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout()); 
        
        JButton joinButton = new JButton("Join New Game");
        joinButton.setFont(new Font("Arial", Font.BOLD, 16));
        joinButton.setPreferredSize(new Dimension(200, 60));
        
        // Add action listener to send join request to server, then show waiting message
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

    // This methods show the playing cards in the current round
    public void displayCards(String message) {
        try {
            // Reset previous cards
            cardsPanel.removeAll();
            
            // Format example: "Cards in the game 100 33 40 132"
            String[] parts = message.split(" ");
            
            // Check if message has the expected format
            if (parts.length >= 5) {

                // Get card numbers starting from index 4
                for (int i = 4; i < parts.length; i++) {
                    int cardNumber = Integer.parseInt(parts[i]);

                    // Create card path
                    String imagePath = "../cards/" + cardNumber + ".gif";
                    
                    // Image loading
                    try {
                        ImageIcon cardIcon = new ImageIcon(imagePath);
                        Image img = cardIcon.getImage();
                        Image scaledImg = img.getScaledInstance(120, 174, Image.SCALE_SMOOTH);
                        cardIcon = new ImageIcon(scaledImg);
                        
                        // Add card to panel
                        JLabel cardLabel = new JLabel(cardIcon);
                        cardLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                        cardsPanel.add(cardLabel);
                        
                    } catch (Exception e) {
                        JLabel cardLabel = new JLabel("Card " + cardNumber);
                        cardLabel.setPreferredSize(new Dimension(120, 174));
                        cardLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        cardLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                        cardsPanel.add(cardLabel);
                    }
                }
            }
            cardsPanel.revalidate();
            cardsPanel.repaint();
        } catch (Exception e) {
            System.err.println("Error displaying cards: " + e.getMessage());
        }
    }
    
    // This methods creates the playing stage interface
    public JPanel createPlayingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Cards panel
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 40));
        cardsPanel.setBackground(Color.WHITE);
        
        // Waiting message
        JLabel waitingLabel = new JLabel("Waiting for cards from the server...", JLabel.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        cardsPanel.add(waitingLabel);
        
        // Input panel and space for equation with submit button
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        equationField = new JTextField();
        equationField.setFont(new Font("Arial", Font.PLAIN, 16));

        JLabel resultLabel = new JLabel("= 0");
        JButton submitButton = new JButton("Submit");

        submitButton.addActionListener(e -> {
            // Evaluate equation then send answer to server
            try {
                String equation = equationField.getText().trim();
                if (!equation.isEmpty()) {
                    try {
                        double result = ExpressionParser.evaluate(equation);
                        String formattedResult = String.format("%.0f", result);
                        resultLabel.setText("= " + formattedResult);
                    } catch (Exception ex) {
                        resultLabel.setText("= Invalid");
                    }
                    player.sendAnswer(equation);
                }
            } catch (Exception ex) {
                System.err.println("Error submitting answer: " + ex.getMessage());
            }
        });
        
        JPanel equationPanel = new JPanel(new BorderLayout(5, 0));
        equationPanel.add(new JLabel("Equation: "), BorderLayout.WEST);
        equationPanel.add(equationField, BorderLayout.CENTER);
        equationPanel.add(resultLabel, BorderLayout.EAST);
        
        inputPanel.add(equationPanel, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        
        panel.add(cardsPanel, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // This method creates the finished stage interface
    public JPanel createFinishedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel gameOverLabel = new JLabel("Game Over!");
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gameOverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (this.message == null || this.message.isEmpty()) {
            this.message = "Game Over! No winner.";
        }

        // Parse the winner message and show the details
        String[] winnerMessage = this.message.split(" ", 3);
        
        JLabel winnerLabel = new JLabel(winnerMessage[1] + " is the winner!");
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        winnerLabel.setForeground(new Color(0, 128, 0)); 
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel expressionLabel = new JLabel(winnerMessage[2]);
        expressionLabel.setFont(new Font("Arial", Font.BOLD, 25));
        expressionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 16));
        playAgainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAgainButton.setMaximumSize(new Dimension(200, 60));
        
        // Add a button to play again
        playAgainButton.addActionListener(e -> {
            try {
                player.joinGame();
                playAgainButton.setEnabled(false);
                playAgainButton.setText("Waiting for players...");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to join game: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(Box.createVerticalGlue());
        panel.add(gameOverLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(winnerLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(expressionLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(playAgainButton);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    // A list to store player information for players panel
    private List<String[]> playersInfo = new ArrayList<>();

    // This method updates the players panel 
    public void updatePlayersPanel(String profile) {
        if (playersInfo == null) {
            playersInfo = new ArrayList<>();
        }
    
        try {
            // Parse profile: "Player profile John 10/30 5.5"
            String[] parts = profile.trim().split(" ");

            if (parts.length >= 3) {

                String name = parts[2];
                String winRate = parts[3];
                String avgTime = parts[4];
                
                // Check if player already exists
                boolean playerFound = false;
                for (int i = 0; i < playersInfo.size(); i++) {
                    if (playersInfo.get(i)[0].equals(name)) {
                        // Update existing player
                        playersInfo.get(i)[1] = winRate;
                        playersInfo.get(i)[2] = avgTime;
                        playerFound = true;
                        break;
                    }
                }
    
                // Add new player if not found
                if (!playerFound) {
                    playersInfo.add(new String[]{name, winRate, avgTime});
                }
                
                // Move current user to the the first row
                for (int i = 0; i < playersInfo.size(); i++) {
                    if (playersInfo.get(i)[0].equals(this.username) && i > 0) {
                        String[] temp = playersInfo.get(i);
                        playersInfo.remove(i);
                        playersInfo.add(0, temp);
                        break;
                    }
                }
                rebuildPlayersPanel();
                }
        } catch (Exception e) {
            System.err.println("Error updating player panel: " + e.getMessage());
        }
    }

    // This method build players panel
    private void rebuildPlayersPanel() {

        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.setPreferredSize(new Dimension(200, 0));
        newPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        newPanel.setBackground(new Color(240, 240, 240));
        
        JLabel playersHeader = new JLabel("Players");
        playersHeader.setFont(new Font("Arial", Font.BOLD, 16));
        playersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPanel.add(playersHeader);
        newPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Add each player information
        for (String[] profile : playersInfo) {
            String name = profile[0];
            String stats = "Win: " + profile[1] + " avg: " + profile[2] + "s";
            boolean isCurrentUser = name.equals(this.username);
            
            JPanel playerPanel = new JPanel();
            playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
            
            // Set background based on if current user
            if (isCurrentUser) {
                playerPanel.setBackground(new Color(230, 255, 230)); 
            } else {
                playerPanel.setBackground(new Color(245, 245, 245));
            }
            
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
            
            newPanel.add(playerPanel);
            newPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        
        // Replace old panel
        if (playersPanel != null) {
            Container parent = playersPanel.getParent();
            if (parent != null) {
                int index = -1;
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    if (parent.getComponent(i) == playersPanel) {
                        index = i;
                        break;
                    }
                }
                
                if (index >= 0) {
                    parent.remove(index);
                    parent.add(newPanel, index);
                    parent.revalidate();
                    parent.repaint();
                }
            }
        }
        
        this.playersPanel = newPanel;
        bodyPanel.add(playersPanel, BorderLayout.EAST);
    }
}