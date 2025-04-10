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


// This class serves as the game server that handles client's requests
public class GameServer extends UnicastRemoteObject implements Server{
    private static final String DB_HOST = "jdbc:mysql://localhost:3306/c3358";
	private static final String DB_USER = "c3358";
	private static final String DB_PASS = "c3358PASS";
    private java.sql.Connection dbConn;
    private String host;

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
		
		// while(true) {
		// 	Message m = queueReceiver.receive();
		// 	if(m != null && m instanceof TextMessage) {
		// 		TextMessage textMessage = (TextMessage)m;
		// 		System.out.println("Received message: "+textMessage.getText());
		// 	} else {
		// 		break;
		// 	}
		// }
	}

    // This function is used to create a message listener
    private void createMessageListener() throws JMSException {
        queueReceiver.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
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
