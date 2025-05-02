import java.rmi.*;
import java.rmi.server.*;
import java.sql.*;
import javax.jms.*;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import java.util.Random;

// This class serves as the game server that handles client's requests
public class JPoker24GameServer extends UnicastRemoteObject implements Server{
    private static final String DB_HOST = "jdbc:mysql://localhost:3306/c3358";
	private static final String DB_USER = "c3358";
	private static final String DB_PASS = "c3358PASS";
    private java.sql.Connection dbConn;
    private String host;
    private boolean gameInProgress = false;
    private long gameStartTime = 0;
    private long gameFinishTime = 0;

    // Store players in the queue for joining a game
    class WaitingPlayer {
        private String username;
        private long joinTime;

        public WaitingPlayer(String username, long joinTime) {
            this.username = username;
            this.joinTime = joinTime;
        }
        public String getUsername() {
            return username;
        }
        public long getJoinTime() {
            return joinTime;
        }

    }

    // The constructor establish connection to DB, and setup queue+topic
    public JPoker24GameServer(String host) throws RemoteException, NamingException, JMSException {
        // Connect to DB 
        try {
            this.dbConn = DriverManager.getConnection(DB_HOST, DB_USER, DB_PASS);
            System.out.println("Database connected!");
        } catch (SQLException e) {
            throw new java.lang.IllegalStateException("Cannot connect the database!", e);
        }

        this.host = host;

        // Setup for topic and queue
		createJNDIContext();
		lookupConnectionFactory();
		lookupQueue();
		createConnection();
        receiveMessageFromQueue();
        setUpTopic();
    }


    // The following attributes and methods are used for the message queue and topic purposes

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicPublisher topicPublisher;
    private Topic topic;
    private TopicConnectionFactory topicFactory;
    private void setUpTopic() {
        try {
            topicFactory = (TopicConnectionFactory)jndiContext.lookup("jms/JPoker24GameConnectionFactory");
            topic = (Topic)jndiContext.lookup("jms/JPoker24GameTopic");
            topicConnection = (TopicConnection) connectionFactory.createConnection();
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            topicPublisher = topicSession.createPublisher(topic);
            topicConnection.start();
        } catch (JMSException e) {
            System.err.println("Failed to create topic connection: " + e);
        } catch (NamingException e) {
            System.err.println("Failed to lookup topic: " + e);
        }
    }

    private Context jndiContext;
	private void createJNDIContext() throws NamingException {
		System.setProperty("org.omg.CORBA.ORBInitialHost", host);
		System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
		try {
			jndiContext = new InitialContext();
		} catch (NamingException e) {
			System.err.println("Could not create JNDI API context: " + e);
			throw e;
		}
	}
	
	private ConnectionFactory connectionFactory;
	private void lookupConnectionFactory() throws NamingException {

		try {
			connectionFactory = (ConnectionFactory)jndiContext.lookup("jms/JPoker24GameConnectionFactory");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS connection factory lookup failed: " + e);
			throw e;
		}
	}
	
	private Queue queue;
	private void lookupQueue() throws NamingException {

		try {
			queue = (Queue)jndiContext.lookup("jms/JPoker24GameQueue");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS queue lookup failed: " + e);
			throw e;
		}
	}

	private javax.jms.Connection connection;
	private void createConnection() throws JMSException {
		try {
			connection = connectionFactory.createConnection();
			connection.start();
		} catch (JMSException e) {
			System.err.println("Failed to create connection to JMS provider: " + e);
			throw e;
		}
	}

    private Session session;
	private void createSession() throws JMSException {
		try {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}
	
	private MessageConsumer queueReceiver;
	private void createReceiver() throws JMSException {
		try {
			queueReceiver = session.createConsumer(queue);
            initGameQueue();
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

    public void receiveMessageFromQueue() throws JMSException {
		createSession();
		createReceiver();
        createMessageListener();
	}

    // The following attributes and methods are used for the lifecycle of the game

    // First, we want to use a Timer to check every second if we are ready to start a new game
    private Timer checkStartGameTimer;
    private static final int CHECK_START_GAME_INTERVAL = 1000; 
    private void startCheckStartGameTimer() {
        checkStartGameTimer = new Timer(true);
        checkStartGameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkStartGame();
            }
        }, 0, CHECK_START_GAME_INTERVAL);
    }

    // This method initiates the list to store all waiting players (from the message received)
    private List<WaitingPlayer> waitingPlayers = new ArrayList<>();
    private void initGameQueue() {
        // Start the timer to check for game start
        startCheckStartGameTimer();
        waitingPlayers = new ArrayList<>();
        System.out.println("Game queue initialized");
    }

    // This method process a join message from player
    private void processJoinMessage(String message) {
        // Parse the message to get username
        String[] parts = message.split(" ");
        String username = parts[0];
        long joinTime = Long.parseLong(parts[1]);

        // Add player to the waiting list
        waitingPlayers.add(new WaitingPlayer(username, joinTime));
        System.out.println("Player " + username + " joined the game queue at time " + joinTime);
        checkStartGame();
    }

    // This method check if a game can be started
    private void checkStartGame() {
        // If there's an ongoing game already, or waiting player is less than 2
        // No need to start any game
        if (gameInProgress || waitingPlayers.size() < 2) {
            return;
        }

        // sort players by wait time to get the earliest join time
        waitingPlayers.sort(Comparator.comparingLong(WaitingPlayer::getJoinTime));

        // immediately start if there are 4 players
        if (waitingPlayers.size() >= 4) {
            startGame(4);
            return;
        } 

        // else check if it's been 10 seconds since the first player joined
        else {
            long currentTime = System.currentTimeMillis()/1000;
            long earliestJoinTime = waitingPlayers.get(0).getJoinTime();
            // if yes, immediately start the game
            if (currentTime - earliestJoinTime >= 10) {
                startGame(Math.min(waitingPlayers.size(), 4));
                return;
            }
        }
    }

    // This method starts the game with the given number of players
    private void startGame(int numPlayers) {
        gameStartTime = System.currentTimeMillis();
        // remove the players from waiting list and store a list of joining players
        List<String> joiningPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            joiningPlayers.add(waitingPlayers.get(i).getUsername());
        }
        for (int i = 0; i < numPlayers; i++) {
            WaitingPlayer player = waitingPlayers.remove(0);
        }

        // update the games_played for all players by adding 1
        updatePlayersDatabase(joiningPlayers);

        try {
            // Notify the players that the game is starting
            TextMessage message = topicSession.createTextMessage("Game is starting");
            topicPublisher.publish(message);
            System.out.println("Starting game with " + numPlayers + " players at " + System.currentTimeMillis()/1000);

            StringBuilder messageUsernames = new StringBuilder();
            for (int i = 0; i < joiningPlayers.size(); i++) {
                messageUsernames.append(joiningPlayers.get(i));
                if (i < joiningPlayers.size() - 1) {
                    messageUsernames.append(" ");
                }
            }
            // Create and send a message contains usernames of participating players
            message = topicSession.createTextMessage("Game started for " + messageUsernames.toString());
            topicPublisher.publish(message);
            System.out.println("Published game start message: " + messageUsernames.toString());
            sendPlayerProfile(joiningPlayers);
            generateRandomCards();
        } catch (JMSException e) {
            System.err.println("Error publishing message: " + e.getMessage());
            e.printStackTrace();
        }
        gameInProgress = true;
    }
	
    // This method is used to send player profile to the players
    private void sendPlayerProfile(List<String> usernames) throws JMSException {
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT username, games_won, games_played, avg_win_time FROM user_info WHERE username = ?");

            for (String username : usernames) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String user = rs.getString("username");
                    int gamesWon = rs.getInt("games_won");
                    int gamesPlayed = rs.getInt("games_played");
                    float avgWinTime = rs.getFloat("avg_win_time");

                    String playerInfo =  String.format("Player info %s %d/%d %.1f", user, gamesWon, gamesPlayed, avgWinTime);
                    TextMessage message = topicSession.createTextMessage(playerInfo);
                    topicPublisher.publish(message);
                    System.out.println("Published player profile: " + user + " " + gamesWon + " " + gamesPlayed + " " + avgWinTime);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving player profile: " + e.getMessage());
            e.printStackTrace();
        } catch (JMSException e) {
            System.err.println("Error publishing message: " + e.getMessage());
            e.printStackTrace();
        }

    }

    // This section is used to generate cards played on the game
    private String[] currentGameCards;
    // Keep track of used rank to avoid repeated value
    private boolean[] usedRank = new boolean[14];
    public void generateRandomCards() {
        Random random = new Random();
        currentGameCards = new String[4];
        for (int i = 0; i < 4; i++) {
            int rank;
            do {
                rank = random.nextInt(13) + 1;
            } while (usedRank[rank]);
            usedRank[rank] = true;
            int suit = random.nextInt(4);
            currentGameCards[i] = String.valueOf(rank) + String.valueOf(suit);
        }
        try {
            TextMessage message = topicSession.createTextMessage("Cards in the game " + String.join(" ", currentGameCards));
            topicPublisher.publish(message);
            System.out.println("Published game cards: " + "Cards in the game " + String.join(" ", currentGameCards)); 
        } catch (JMSException e) {
            System.err.println("Error publishing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // This methods create a message listener 
    // and process different types of message from the queue
    private void createMessageListener() throws JMSException {
        queueReceiver.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String text = textMessage.getText();
                        // This type of message is an answer published by the client
                        if (text.startsWith("Answer ")) {
                            String[] answers = text.split(" ");
                            // Using the ExpressionParser class, check if the answer is correct
                            if (!ExpressionParser.isCorrect24Solution(answers[3])) {
                                System.out.println("Incorrect answer: " + text);
                            } else {
                                // If answer is correct, end the game
                                gameFinishTime = System.currentTimeMillis();
                                gameInProgress = false;
                                // Update database and leaderboard
                                updateWinnerDatabase(gameFinishTime - gameStartTime, answers[1]);
                                updateLeaderboard();
                                System.out.println("Correct answer: " + text);                    
                                String winnerInfo =  "Winner " + answers[1] + " " + answers[3];
                                // Announce the winner to all players
                                message = topicSession.createTextMessage(winnerInfo);
                                topicPublisher.publish(message);
                                System.out.println("Published winner : " + winnerInfo);
                            }
                        } else {
                            // This type of message is a join message from the client
                            processJoinMessage(text);
                            System.out.println("Received message: " + textMessage.getText());
                        }
                    } else {
                        System.out.println("Received non-text message");
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // The following methods are used to update the database
    // synchronized is used to retain consistency across server

	
    private synchronized void updatePlayersDatabase(List<String> usernames) {
        try {
            for (String username : usernames) {
                PreparedStatement stmt = this.dbConn.prepareStatement("UPDATE user_info SET games_played = games_played + 1 WHERE username = ?");
                stmt.setString(1, username);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateWinnerDatabase(long gameTime, String winner) {
        double winTime = gameTime / 1000.0;
        // Update the winner's games won and average win time
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT games_won, avg_win_time FROM user_info WHERE username = ?");
            stmt.setString(1, winner);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int gamesWon = rs.getInt("games_won");
                double avgWinTime = rs.getDouble("avg_win_time");
                double newAvgWinTime = (avgWinTime * gamesWon + winTime) / (gamesWon + 1);
                String updateQuery = "UPDATE user_info SET games_won = ?, avg_win_time = ? WHERE username = ?";
                PreparedStatement updateStmt = this.dbConn.prepareStatement(updateQuery);
                updateStmt.setInt(1, gamesWon + 1);
                updateStmt.setDouble(2, newAvgWinTime);
                updateStmt.setString(3, winner);
                updateStmt.executeUpdate();
            }
        }   catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateLeaderboard() {
        List<String> rankedUsernames = new ArrayList<>();

        try {
            // Get the top players based on games won
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT username FROM user_info ORDER BY games_won DESC");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rankedUsernames.add(rs.getString("username"));
            }

            
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Update rank of each player
        for (int i = 0; i < rankedUsernames.size(); i++) {
            String name = rankedUsernames.get(i);
            int rank = i + 1;

            try {
                PreparedStatement stmt = this.dbConn.prepareStatement("UPDATE user_info SET `rank` = ? WHERE username = ?");
                stmt.setInt(1, rank);
                stmt.setString(2, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // The methods below handle user authentication

    // This function handles login request from the client
    public synchronized boolean login(String username, String password) throws RemoteException {
        //System.out.println("Login request for: " + username);

        boolean isPasswordCorrect = false;
        boolean isNotLoggedIn = false;

        // Read a row from user_info, matching username and password
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT password FROM user_info WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                if (dbPassword.equals(password)) {
                    isPasswordCorrect = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Read a row from online_user, matching if username exists
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT username FROM online_user WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                isNotLoggedIn = true;
            }
        }   catch (SQLException e) {
            e.printStackTrace();
        }

        // If password is correct and user is not logged in, add to online_user
        if (isPasswordCorrect && isNotLoggedIn) {
            try {
                PreparedStatement stmt = this.dbConn.prepareStatement("INSERT INTO online_user (username) VALUES (?)");
                stmt.setString(1, username);
                stmt.execute();
                //System.out.println("Users added to ONLINE USERS");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //System.out.println("Logged in as " + username);
            return true;
        }
        return false;
    }

    // This function handles register (sign up) request from the client
    public synchronized boolean register(String username, String password) throws RemoteException {
        //System.out.println("Registration request for: " + username);

        // Check if username alread exists in user_info to avoid duplicate username
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("SELECT username FROM user_info WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Insert into user_info
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("INSERT INTO user_info (username, password) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.execute();
            System.out.println("User added to USER INFO");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Insert into online_user
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("INSERT INTO online_user (username) VALUES (?)");
            stmt.setString(1, username);
            stmt.execute();
            System.out.println("User added to ONLINE USERS");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        //System.out.println("Registered and logged in as " + username);
        return true;
    }

    // This function handles logout request from the client
    public synchronized boolean logout(String username) throws RemoteException {
        //System.out.println("Logout request for: " + username);

        // Update online_user. Remove the username from the list
        // Assumption: if there is a request from a username that does not even exist in the table initially, it is still considered a successful logout
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement("DELETE FROM online_user WHERE username = ?");
            stmt.setString(1, username);
            stmt.execute();
            System.out.println("Users removed from ONLINE USERS");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public static void main(String[] args) {
        System.out.println("Server started");
        String host = "localhost";
		JPoker24GameServer app = null;
        try {
            app = new JPoker24GameServer(host);
            System.setSecurityManager(new SecurityManager());
            Naming.rebind("JPoker24GameServer", app);
            System.out.println("Service registered");
        } catch (Exception e) {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        } 
    }

}
