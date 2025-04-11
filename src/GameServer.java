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

// This class serves as the game server that handles client's requests
public class GameServer extends UnicastRemoteObject implements Server{
    private static final String DB_HOST = "jdbc:mysql://localhost:3306/c3358";
	private static final String DB_USER = "c3358";
	private static final String DB_PASS = "c3358PASS";
    private java.sql.Connection dbConn;
    private String host;
    private boolean gameInProgress = false;

    public GameServer(String host) throws RemoteException, NamingException, JMSException {
        // Connect to DB 
        try {
            this.dbConn = DriverManager.getConnection(DB_HOST, DB_USER, DB_PASS);
            System.out.println("Database connected!");
        } catch (SQLException e) {
            throw new java.lang.IllegalStateException("Cannot connect the database!", e);
        }

        this.host = host;
		// Access JNDI
		createJNDIContext();
		// Lookup JMS resources
		lookupConnectionFactory();
		lookupQueue();
		// Create connection->session->sender
		createConnection();
        receiveMessageFromQueue();

        setUpTopic();
    }

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

    private Timer checkStartGameTimer;
    private static final int CHECK_START_GAME_INTERVAL = 1000; // 1 second
    private void startCheckStartGameTimer() {
        checkStartGameTimer = new Timer(true);
        checkStartGameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkStartGame();
            }
        }, 0, CHECK_START_GAME_INTERVAL);
    }

    private List<WaitingPlayer> waitingPlayers = new ArrayList<>();
    private void initGameQueue() {
        // Start the timer to check for game start
        startCheckStartGameTimer();
        waitingPlayers = new ArrayList<>();
        System.out.println("Game queue initialized");
    }

    private void processJoinMessage(String message) {
        // Parse the message to get the username
        String[] parts = message.split(" ");
        String username = parts[0];
        long joinTime = Long.parseLong(parts[1]);

        // Add the player to the waiting list
        waitingPlayers.add(new WaitingPlayer(username, joinTime));
        System.out.println("Player " + username + " joined the game queue at time " + joinTime);
        checkStartGame();
    }

    private void checkStartGame() {
        if (gameInProgress || waitingPlayers.size() < 2) {
            return;
        }
        // sort players by wait time
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
            if (currentTime - earliestJoinTime >= 10) {
                startGame(Math.min(waitingPlayers.size(), 4));
                return;
            }
        }
    }

    private void startGame(int numPlayers) {
        // remove the players from waiting list
        // while also store a list of joining players
        // and notify them that the game is starting
        List<String> joiningPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            joiningPlayers.add(waitingPlayers.get(i).getUsername());
        }
        for (int i = 0; i < numPlayers; i++) {
            WaitingPlayer player = waitingPlayers.remove(0);
        }


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
            // Create and send the message
            message = topicSession.createTextMessage(messageUsernames.toString());
            topicPublisher.publish(message);
            System.out.println("Published game start message: " + messageUsernames.toString());
        } catch (JMSException e) {
            System.err.println("Error publishing message: " + e.getMessage());
            e.printStackTrace();
        }

        //gameInProgress = true;
    }

    // The following attributes and methods are used for the message queue purposes
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

    // private Topic topic;
    // private void lookupTopic() throws NamingException {

    //     try {
    //         queue = (Queue)jndiContext.lookup("jms/JPoker24GameTopic");
    //     } catch (NamingException e) {
    //         System.err.println("JNDI API JMS topic lookup failed: " + e);
    //         throw e;
    //     }
    // }
	
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
	
	public void receiveMessageFromQueue() throws JMSException {
		createSession();
		createReceiver();
        createMessageListener();
	}

    // This function is used to create a message listener
    private void createMessageListener() throws JMSException {
        queueReceiver.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        processJoinMessage(textMessage.getText());
                        System.out.println("Received message: " + textMessage.getText());
                    } else {
                        System.out.println("Received non-text message");
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
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
	
	// Was: QueueReceiver
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

    // synchronized is used to retain consistency across server

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
		GameServer app = null;
        try {
            app = new GameServer(host);
            System.setSecurityManager(new SecurityManager());
            Naming.rebind("GameServer", app);
            System.out.println("Service registered");

            //keepAlive(app);

        } catch (Exception e) {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    // public static void keepAlive(final GameServer app) {
    //    Runtime.getRuntime().addShutdownHook(new Thread() {
    //         public void run() {
    //             app.close();
    //             System.out.println("Server closed");
    //         }
    //     });
    // }

    // public void close() {
    //     System.out.println("Closing server resources...");
    //     try {
    //         if (queueReceiver != null) {
    //             queueReceiver.close();
    //         }
    //         if (session != null) {
    //             session.close();
    //         }
    //         if (connection != null) {
    //             connection.close();
    //         }
    //         if (dbConn != null) {
    //             dbConn.close();
    //         }
    //         System.out.println("All resources closed successfully");
    //     } catch (Exception e) { 
    //         System.err.println("Error closing resources: " + e.getMessage());
    //         e.printStackTrace();
    //     }
		
	// }
}
