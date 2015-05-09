# Networking Project
##### by Caleb F, Jesse F, Reece N and Alexander PA

## [0] TODO LIST
#### 0.1 High priority
- ***Collector***: *Write some data for the collectors to collect*
- ***Analyst***: *Design the analysis types; return data*
- ***Retry instead of crashing*** - Handle exceptions nicely, *pause/retry/hang connection* instead of crashing.

#### 0.2 Medium priority
- ***Add user input if required*** - add console/user commands IF necessary on some applications.
  - *alt*. ***Slow down process*** - perhaps with *sleep(1000)* for the demo
- ***Make the code readable from a distance*** - Lots of random code, and lines that are poorly worded, unused, over-commented, etc.

#### 0.3 Low priority
- ***Create a pretty SSL cert*** - Potentially give different servers different server/client SSL certificates.
- ***Split code into subclasses*** - *WHERE NECESSARY* - For readability, encapsulation, modularity, etc.

## [1] Core Components
### 1.0 Description
The project consists of four root components:
- <i>**[The Analysts (1.1)](/Analyst.java)**: Nodes that recieve raw data and return a result/action (for the price of 1 eCent).</i>
- <i>**[The Bank (1.2)](/Bank.java)**: A single node that dishes out money to the collectors, and stores accounts for the analysts.</i>
- <i>**[The Collectors (1.3)](/Collector.java)**: Nodes that collect raw data and send it to the director for processing.</i>
- <i>**[The Director (1.4)](/Director.java)**: A "bridging" delegate node that forwards collectors requests for analysis.</i>

### 1.1 Analysts
- <code>Analyst() throws IOException</code> : Runs the analyst program
- <code>analyse( AnalysisType )</code> : ...
- <code>getMoney()</code> : ...
- <code>depositECent( eCent )</code> : Connects to Bank, deposits eCent
- <code>initDir()</code> : Connect to director
- <code>getIPAddress()</code>

### 1.2 Bank
- <code>generateECent()</code> : Creates an eCent in memory
- <code>getSHA256Hash( codeword )</code>
- <code>getSalt()</code>

### 1.3 Collectors
- <code>Collector() throws IOException</code> : Runs the collector program
- <code>buyMoney()</code> : Connects to Bank, purchases money
- <code>initDir()</code> : Connect to director
- <code>sendData()</code> : Sends data to Director for analysis
- <code>collect()</code> : returns <code>int[]</code>

### 1.4 Director
- <code>Director( int portNo )</code> : Creates an instance of the director
- <code>startSocket (int portNo )</code> : Starts the socket (**returns true/false**)
- <code>getIPAddress()</code>


## [2] Helper classes and ADTs
### 2.0 Description
The project also utilises a number of *Abstract Data Types* and helper classes
- <i>**[ECentWallet (ADT) (1.1)](/lib/ECentWallet.java)**: Holds the users eCent balance (as well as inidividual eCents) in volatile AND hard memory.</i>
- <i>**[Message (ADT) (1.2)](/lib/Message.java)**: A wrapper for raw string data; provides protocol for reading/formatting messages.</i>
- <i>**[MessageFlag (Helper) (1.3)](/lib/MessageFlag.java)**: A globally accessible series of flags that denote Message format.</i>
- <i>**[SSLHandler (Helper) (1.4)](/lib/SSLHandler.java)**: Helper methods for consistency in declaring SSL Certificates</i>
