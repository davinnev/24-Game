import javax.swing.*;
import java.awt.*;

// This is an abstract class for the login and register page
public abstract class PlayerAuth extends JPanel {
    protected GamePlayer player;
    protected Server server;
    protected JLabel titleLabel;
    protected JLabel errorLabel;
    
    // A constructor to the page, given the client and server object
    public PlayerAuth(GamePlayer player, Server server) {
        this.player = player;
        this.server = server;
        this.setLayout(new BorderLayout());

        setOpaque(false);
        player.getFrame().getContentPane().setBackground(new Color(220, 255, 220));

        // Create title
        this.titleLabel = new JLabel(getTitle(), JLabel.CENTER);
        this.titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        this.titleLabel.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));
        this.titleLabel.setOpaque(false);
        add(titleLabel, BorderLayout.NORTH);        
        
        // Create error label
        this.errorLabel = new JLabel("", JLabel.CENTER);
        this.errorLabel.setBackground(Color.RED);        
        this.errorLabel.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        this.errorLabel.setOpaque(false);
        add(errorLabel, BorderLayout.SOUTH);     

        // Create and add form panel, content of form depends on whether it's login/register
        JPanel formPanel = drawForm();
        add(formPanel, BorderLayout.CENTER);
        
    }

    // To be implemented by the login and register page classes
    public abstract String getTitle();
    public abstract JPanel drawForm();
    public abstract void clearText();

    public void setError(String error) {
        this.errorLabel.setText(error);
    }

    public void clearError() {
        this.errorLabel.setText(" ");
    }
}
