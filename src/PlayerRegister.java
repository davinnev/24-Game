import javax.swing.*; 
import java.awt.*;
import java.rmi.*;

// This class serves as the UI for the register page
public class PlayerRegister extends PlayerAuth{
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    
    // Constructor following the superclass
    public PlayerRegister(JPoker24Game player, Server server) {
        super(player, server);
    }

    @Override
    public String getTitle() {
        return "Register";
    }

    @Override
    public JPanel drawForm() {

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,30,10,30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Username field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);
        
        // Confirm password field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Confirm password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        confirmPasswordField = new JPasswordField(15);
        panel.add(confirmPasswordField, gbc);
                
        
        // Buttons panel and the buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton registerButton = new JButton("Register");
        // If user click the register button, send the request to the server
        registerButton.addActionListener(e -> handleRegister());
        // If user click the login button, change the UI to the login page
        JButton backButton = new JButton("Back to Login");
        backButton.addActionListener(e -> this.player.showLogin());
        buttonsPanel.add(registerButton);
        buttonsPanel.add(backButton);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buttonsPanel, gbc);
        
        return panel;
    }

    @Override
    public void clearText() {
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    // This function sends the register request to the server
    public void handleRegister() {
        //System.out.println("Username from register panel: " + usernameField.getText());
        String usernameInput = this.usernameField.getText();
        String passwordInput = new String(this.passwordField.getPassword());
        String confirmPasswordInput = new String(this.confirmPasswordField.getPassword());
        // Call RMI function
        try {
        
            if (!usernameInput.isEmpty() && !passwordInput.isEmpty() && passwordInput.equals(confirmPasswordInput)) {
                boolean isReg = this.server.register(usernameInput, passwordInput);
        
                if (!isReg) {
                    // Register fails, username has been taken
                    JOptionPane.showMessageDialog(
                            this, // or whatever your parent component is
                            "Username already exists. Please try again.",
                            "Register Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                    clearText();
                } else {
                    // If register successful take the client to the game page
                    player.setUsername(usernameInput);
                    player.showProfile();
                }
            }
             // Handle errors if username/password is empty or the password and confirmPassword don't match
            else if (usernameInput.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Login name should not be empty");
            }
            else if (passwordInput.isEmpty() || confirmPasswordInput.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Password should not be empty");
            }
            if (!passwordInput.equals(confirmPasswordInput)) {
                JOptionPane.showMessageDialog(null, "Passwords do not match");
            }
      } catch (RemoteException error) {
          System.err.println("Failed invoking RMI: ");
      }
    }
}
