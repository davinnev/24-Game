import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.Message;


// This class serves as the client as a player in the game
public class JPoker24Game implements Runnable  {

    private Server gameServer;
    private JFrame frame;
    private PlayerLogin loginPanel;
    private PlayerRegister registerPanel;
    private Profile profilePanel;
    private Leaderboard leaderboardPanel;
    private MainGame gamePanel;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private String username;
    private String host;

    // Getter function for username
    public String getUsername() {
        return username;
    }

    // Setter function for username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter function for frame
    public JFrame getFrame() {
        return frame;
    }

    // Function to set frame size
    public void setFrameSize(int width, int height) {
        frame.setSize(width, height);
    }

    // This function helps with connecting with the RMI registry service
    // Also to set up the connection with JMS queue
    public JPoker24Game(String host) throws NamingException, JMSException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            this.gameServer = (Server)registry.lookup("JPoker24GameServer");
            System.out.println("Server found" + this.gameServer);
        } catch (Exception e){
            System.err.println("Error accessing RMI: " + e.toString());
        }
        this.host = host;
        
        createJNDIContext();
        lookupConnectionFactory();
        lookupQueue();
        setUpTopic();
    }

    // This part is used to setup queue and topic

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
    
    private Connection connection;
    private void createConnection() throws JMSException {
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException e) {
            System.err.println("Failed to create connection to JMS provider: " + e);
            throw e;
        }
    }

    public void sendMessageToQueue() throws JMSException {
        createConnection();
        createSession();
        createSender();			
        TextMessage message = session.createTextMessage(); 
        String messageContent = this.username + " " + String.valueOf(System.currentTimeMillis() / 1000); 
        message.setText(messageContent);
        queueSender.send(message);
        System.out.println("Sending message "+ messageContent);
        // send non-text control message to end
        queueSender.send(session.createMessage());
    }

    private Session session;
    private void createSession() throws JMSException {
        System.out.println(connection.toString());
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            System.err.println("Failed to create session: " + e);
            throw e;
        }
    }
    
    private MessageProducer queueSender;
    private void createSender() throws JMSException {
        try {
            queueSender = session.createProducer(queue);
        } catch (JMSException e) {
            System.err.println("Failed to create sender: " + e);
            throw e;
        }
    }

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicSubscriber topicSubscriber;
    private Topic topic;
    private TopicConnectionFactory topicFactory;
    private void setUpTopic() {
        try {
            topicFactory = (TopicConnectionFactory)jndiContext.lookup("jms/JPoker24GameConnectionFactory");
            topic = (Topic)jndiContext.lookup("jms/JPoker24GameTopic");
            topicConnection = (TopicConnection) connectionFactory.createConnection();
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            topicSubscriber = topicSession.createSubscriber(topic);

            topicSubscriber.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            String text = textMessage.getText();

                            if (text.startsWith("Game started for ")) {
                                String[] usernames = text.split(" ");
                                for (String uname : usernames) {
                                    if (JPoker24Game.this.getUsername().trim().equals(uname.trim())) {
                                        System.out.println("You are already in the game");
                                        gamePanel.setGameState("PLAYING");
                                        return;
                                    }
                                }
                            } else if (text.startsWith("Player info ")) {
                                 JPoker24Game.this.gamePanel.updatePlayersPanel(text);
                            } else if (text.startsWith("Cards in the game ") && gamePanel.getGameState().equals("PLAYING")) {
                                JPoker24Game.this.gamePanel.displayCards(text);
                            } else if (text.startsWith("Winner ")) {
                                // show the game result
                                // System.out.println("Received winner: " + text);
                                gamePanel.setMessage(text);
                                System.out.println("Panel winner: " + gamePanel.getMessage());
                                gamePanel.setGameState("FINISHED");
                            } else {
                                System.out.println("Received message: " + text);
                            }
                        } else {
                            System.out.println("Received non-text message");
                        }
                    } catch (JMSException e) {
                        System.err.println("Failed to process message: " + e);
                    }
                }
            });
            topicConnection.start();
        } catch (JMSException e) {
            System.err.println("Failed to create topic connection: " + e);
        } catch (NamingException e) {
            System.err.println("Failed to lookup topic: " + e);
        }
    }


    public static void main(String[] args){
        String host = "localhost";
        JPoker24Game player = null;
        try {
            player = new JPoker24Game(host);
            SwingUtilities.invokeLater(player);
        } catch (NamingException | JMSException e) {
            System.err.println("Program aborted");
        } finally {
            if(player != null) {
                try {
                    player.close();
                } catch (Exception e) { }
            }
        }
    }

    // This method is used to submit answer during game-playing stage
    public void sendAnswer(String answer) {
        try {
            TextMessage message = session.createTextMessage(); 
            String messageContent = "Answer " + this.username + " " + System.currentTimeMillis()/1000 + " " + answer; 
            message.setText(messageContent);
            queueSender.send(message);
            System.out.println("Sending message "+ messageContent);
        } catch (JMSException e) {
            System.err.println("Failed to send message: " + e);
        }
    }

    // The "show.." functions below help to transition between different UI pages

    public void showLogin() {
        cardLayout.show(cardPanel, "login");
        frame.setTitle("Login");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        loginPanel.clearText();
        loginPanel.clearError();
    }

    public void showRegister() {
        cardLayout.show(cardPanel, "register");
        frame.setTitle("Register");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        registerPanel.clearText();
        registerPanel.clearError();
    }

    public void showProfile() {
        if (profilePanel == null) {
            profilePanel = new Profile(this, gameServer);
            cardPanel.add(profilePanel, "profile");
        }
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);
        profilePanel.highlightActiveTab(profilePanel.profileTab);
        cardLayout.show(cardPanel, "profile");
        profilePanel.refreshProfile();
    }

    public void showLeaderboard() {
        if (leaderboardPanel == null) {
            leaderboardPanel = new Leaderboard(this, gameServer);
            cardPanel.add(leaderboardPanel, "leaderboard");
        }
        frame.setSize(1000, 600);
        leaderboardPanel.highlightActiveTab(leaderboardPanel.leaderboardTab);
        cardLayout.show(cardPanel, "leaderboard");
        leaderboardPanel.refreshLeaderboard();
    }

    public void showMainGame() {
        if (gamePanel == null) {
            gamePanel = new MainGame(this, gameServer);
            cardPanel.add(gamePanel, "game");
        }
        frame.setSize(1000, 600);
        gamePanel.highlightActiveTab(gamePanel.gameTab);
        cardLayout.show(cardPanel, "game");
    }

    // This method helps to send a join request message to the server
    public void joinGame() throws JMSException {
        sendMessageToQueue();
    }
    
    // This function helps initiating the UI on the auth page
    public void setupFrame(){

        // Main frame of the game
        frame = new JFrame("24-Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.getContentPane().setBackground(new Color(220, 255, 220));

        // CardLayout to switch between login and register
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        frame.add(cardPanel);
        frame.setLocationRelativeTo(null);
    }

    public void drawAuthPanel(){
        loginPanel = new PlayerLogin(this, gameServer);
        registerPanel = new PlayerRegister(this, gameServer);
        cardPanel.add(loginPanel, "login");
        cardPanel.add(registerPanel, "register");
        cardLayout.show(cardPanel, "login");
    }

    public void run() {
        setupFrame();
        drawAuthPanel();
        frame.setVisible(true);
    }

    // We call this function every time a user logouts
    public void reset() {
        this.setUsername(null);

        if (profilePanel != null) {
            cardPanel.remove(profilePanel);
            profilePanel = null;
        }
        if (leaderboardPanel != null) {
            cardPanel.remove(leaderboardPanel);
            leaderboardPanel = null;
        }
        if (gamePanel != null) {
            cardPanel.remove(gamePanel);
            gamePanel = null;
        }
    }

    public void close() {
        if(connection != null) {
            try {
                connection.close();
            } catch (JMSException e) { }
        }
    }
}