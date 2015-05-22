# Networking Project
##### Reece Notargiacomo


*By default Analysts and Collectors will try to connect to a Director and Bank on <code>localhost:9998</code> and <code>localhost:9999</code>*

##0 - Setup

####0.1 Generate keypairs: http://docs.oracle.com/javaee/5/tutorial/doc/bnbxw.html
- 1. Generate the keystore (distribute to bank and director)
- 2. Generate the certificate
- 3. Turn cert into truststore (distribute to analysts/collectors)

####0.2 Program options
- **Compile** - <code>javac *.java</code>
- **Director** - <code>java Director -port=3000 -sslcert=KeyStore.jks -sslpass=PassWord123 -demomode -nocolour</code>
- **Bank** - <code>java Bank -bankvault=bank.wallet -port=3001 -sslcert=KeyStore.jks -sslpass=PassWord123 -demomode -nocolour</code>
- **Analysts** - <code>java Analyst NAV (or ORC) -dir=localhost:9000 -bank=192.168.1.1 -sslcert -demomode -nocolour</code>
- **Collector** - <code>java Collector -dir=localhost:9000 -bank=192.168.1.1 -sslcert -demomode -nocolour</code>

##1 - Core Components
###1.0 Description
The project consists of four distinct pieces of software:
- <i>**[The Analysts (1.1)](/Analyst.java)**: Nodes that receive raw data and return a result/action (for the price of 1 eCent).</i>
  - In this demonstration, there are two main analyst types:
    - **[NAV] Navigator**: A node that assists the collector in traversing it's environment.
    - **[ORC] Obstruction-Response Coordinator**: A node that assists the collector in dealing with unexpected obstructuions.
  - <i>**[The Bank (1.2)](/Bank.java)**: A single node that dishes out money to the collectors, and stores accounts for the analysts.</i>
  - <i>**[The Collectors (1.3)](/Collector.java)**: Nodes that collect raw data and send it to the director for processing.</i>
    - **Autonomous Robot AI**: In this demonstration, the collector node is a "blind" robot that needs help traversing and analysing it's environment.
  - <i>**[The Director (1.4)](/Director.java)**: A "bridging" delegate node that forwards collectors requests for analysis.</i>


##2 - Helper classes and ADTs
###2.0 Description
The project also utilises a number of *Abstract Data Types* and helper classes
- <i>**[ECentWallet (ADT) (1.1)](/lib/ECentWallet.java)**: Holds the an eCent balance (as well as inidividual eCents) in volatile AND hard memory.</i>
- <i>**[Message (ADT) (1.2)](/lib/Message.java)**: A wrapper for raw data; provides protocol for reading/formatting messages.</i>
- <i>**[SocketConnection (ADT) (1.3)](/lib/SocketConnection.java)**: A container class that provides helpful functionality for socket connections.</i>
- <i>**[Security (Helper) (1.4)](/lib/Security.java)**: Provides a basic framework for accessing the security features of SSL encryption.</i>
- <i>**[Node (Helper) (1.5)](/lib/Node.java)**: Provides basic display/output information and helper methods.</i>
