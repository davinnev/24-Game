The application’s structure is as such (cards folder includes all the card pictures used in the game):

24-Game
├── cards
├── security.policy
└── src
  ├── ExpressionParser.java
  ├── GameLobby.java
  ├── JPoker24Game.java
  ├── JPoker24GameServer.java
  ├── Leaderboard.java
  ├── MainGame.java
  ├── PlayerAuth.java
  ├── PlayerLogin.java
  ├── PlayerRegister.java
  ├── Profile.java
  └── Server.java

** Running the application **
*** a. Server side ***
First, run the Glassfish service. For example (depends on path to Glassfish):

cd glassfish5/glassfish/bin
./asadmin start-domain

Then, back to the ./src folder, export the correct CLASSPATH for glassfish and database:

export CLASSPATH=:/path/to/glassfish5/glassfish/lib/gf-client.jar:/path/to/mysql-connector-j-9.2.0.jar

Start the RMI registry: rmiregistry &
Compile the Java code: javac *.java
Start the server: java -Djava.security.policy=../security.policyJPoker24GameServer

*** b. Client side ***
Go to src folder and export the correct CLASSPATH for glassfish and database:

export CLASSPATH=.:/path/to/glassfish5/glassfish/lib/gf-client.jar:/path/to/mysql-connector-j-9.2.0.jar

Start the client: java -Djava.security.policy=../security.policyJPoker24Game
