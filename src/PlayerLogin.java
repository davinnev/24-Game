import javax.swing.*; 
import java.awt.*;
import java.rmi.*;

// This class serves as the UI for the login page
public class PlayerLogin extends PlayerAuth{
    private JTextField usernameField;
    private JPasswordField passwordField;
    
    // Constructor following the superclass
    public PlayerLogin(GamePlayer player, Server server) {
        super(player, server);
    }

    @Override
    public String getTitle() {
        return "Login";
    }

    @Override
    public JPanel drawForm() {

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,30,10,30));
        panel.setOpaque(false);

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

        // Buttons panel and the buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loginButton = new JButton("Login");
        // If user click the login button, send the request to the server
        loginButton.addActionListener(e -> handleLogin());
        // If user click the register button, change the UI to the register page
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> player.showRegister());
        buttonsPanel.add(loginButton);
        buttonsPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buttonsPanel, gbc);
        
        return panel;
    }

    @Override
    public void clearText() {
        usernameField.setText("");
        passwordField.setText("");
    }

    // This function sends the login request to the server
    public void handleLogin() {
        //System.out.println("Username from login panel: " + usernameField.getText());
        String usernameInput = this.usernameField.getText();
        String passwordInput = new String(this.passwordField.getPassword());
        // Call RMI function
        try {
            if (!usernameInput.isEmpty() && !passwordInput.isEmpty()) {
                boolean isLogin = this.server.login(usernameInput, passwordInput);
                if (!isLogin) {
                    // Login fails, show a text saying "Incorrect username/password"
                    JOptionPane.showMessageDialog(
                            this, 
                            "Incorrect username or password. Or you might have been logged in already. Please try again.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                   clearText();
                } else {
                    // If login successful take the client to the game page
                    player.setUsername(usernameInput);
                    player.showProfile();
                }
            }
            // Handle errors if username/password is empty
            else if (usernameInput.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Login name should not be empty");
            }
            else if (passwordInput.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Password should not be empty");
            }
        } catch (RemoteException error) {
            System.err.println("Failed invoking RMI: ");
        }
    }
}
