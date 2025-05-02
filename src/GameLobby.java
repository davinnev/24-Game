import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.sql.*;


// This class is an abstraction to the game page
public abstract class GameLobby extends JPanel {
	protected GamePlayer player;
	protected Server server;
	protected String username;

	// Main panels 
	protected JPanel headerPanel;
	protected JPanel bodyPanel;

	// Tabs
	protected JButton profileTab;
	protected JButton gameTab;
	protected JButton leaderboardTab;
	protected JButton logoutTab;

	// For DB connection
	private static final String DB_HOST = "jdbc:mysql://localhost:3306/c3358";
	private static final String DB_USER = "c3358";
	private static final String DB_PASS = "c3358PASS";
	private java.sql.Connection dbConn;

	// This function helps to change the current player if the client logout and re-login with different account
	public void setUsername() {
		this.username = player.getUsername();
	}
	
	// This function acts as a getter for the username
	public String getUsername() {
		return this.username;
	}

	// This function acts as a getter for the DB connection, used by the panels / subclasses
	public Connection getDBConn() {
		return this.dbConn;
	}

	// The constructor takes the client and server object
	public GameLobby(GamePlayer player, Server server) {
		try {
				this.dbConn = DriverManager.getConnection(DB_HOST, DB_USER, DB_PASS);
				System.out.println("Database connected!");
		} catch (SQLException e) {
				throw new java.lang.IllegalStateException("Cannot connect the database!", e);
		}

		this.player = player;
		this.server = server;
		this.username = player.getUsername();

		this.player.getFrame().setTitle("JPoker 24-Game");
		this.player.setFrameSize(1000, 600);
		this.player.getFrame().setLocationRelativeTo(null);
		setLayout(new BorderLayout());

		// Create header panel
		headerPanel = new JPanel();
		headerPanel.setLayout(new GridLayout(1, 4));
		// Create body panel
		bodyPanel = new JPanel();
		bodyPanel.setBackground(new Color(230,230,230));
		bodyPanel.setLayout(new BorderLayout());
		add(bodyPanel, BorderLayout.CENTER);

		// Create and add tabs to panel
		profileTab = new JButton("User Profile");
		headerPanel.add(profileTab);
		gameTab = new JButton("Play Game");
		headerPanel.add(gameTab);
		leaderboardTab = new JButton("Leaderboard");
		headerPanel.add(leaderboardTab);
		logoutTab = new JButton("Logout");
		headerPanel.add(logoutTab);

		add(headerPanel, BorderLayout.NORTH);
		add(bodyPanel, BorderLayout.CENTER);

		// Add action listeners to tabs
		profileTab.addActionListener(e -> player.showProfile());
		gameTab.addActionListener(e -> player.showMainGame());
		leaderboardTab.addActionListener(e -> player.showLeaderboard());
		// Add handler if user clicks logout
		logoutTab.addActionListener(e -> {
			try {
				boolean isLogout = server.logout(username);
				if (isLogout == true) {
					player.reset();
					player.showLogin();
				}
			} catch (RemoteException error) {
				 System.err.println("Failed invoking RMI: ");
			}
		});
		initializeContent();
	}

	// This function highlights active tab 
	public void highlightActiveTab(JButton activeTab) {
		profileTab.setFont(new Font("Arial", Font.PLAIN, 15));
		gameTab.setFont(new Font("Arial", Font.PLAIN, 15));
		leaderboardTab.setFont(new Font("Arial", Font.PLAIN, 15));
		logoutTab.setFont(new Font("Arial", Font.PLAIN, 15));
		activeTab.setFont(new Font("Arial", Font.BOLD, 15));
	}

	// To be overridden, content depends on active tab
	protected abstract void initializeContent();
}
