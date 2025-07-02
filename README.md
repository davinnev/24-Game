# 24-Game

## 📁 Project Structure

The application's structure is as follows (the `cards` folder includes all the card pictures used in the game):

```
24-Game/
├── cards/
├── security.policy
├── src/
│   ├── ExpressionParser.java
│   ├── GameLobby.java
│   ├── JPoker24Game.java
│   ├── JPoker24GameServer.java
│   ├── Leaderboard.java
│   ├── MainGame.java
│   ├── PlayerAuth.java
│   ├── PlayerLogin.java
│   ├── PlayerRegister.java
│   ├── Profile.java
│   └── Server.java
```

---

## 🚀 Running the Application

### a. **Server Side**

1. **Start the Glassfish service** (path may vary depending on installation):

```bash
cd glassfish5/glassfish/bin
./asadmin start-domain
```

2. **Set the `CLASSPATH` for Glassfish and MySQL Connector**  
   From your `./src` directory:

```bash
export CLASSPATH=.:/path/to/glassfish5/glassfish/lib/gf-client.jar:/path/to/mysql-connector-j-9.2.0.jar
```

3. **Start the RMI registry & run the server:**

```bash
rmiregistry
javac *.java
java -Djava.security.policy=../security.policy JPoker24GameServer
```

---

### b. **Client Side**

1. **From the `./src` folder, set the `CLASSPATH` again:**

```bash
export CLASSPATH=.:/path/to/glassfish5/glassfish/lib/gf-client.jar:/path/to/mysql-connector-j-9.2.0.jar
```

2. **Run the client:**

```bash
java -Djava.security.policy=../security.policy JPoker24Game
```

---
