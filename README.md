# TURING
TURING (aka _dis**T**rib**U**ted collabo**R**ative ed**I**ti**NG**_) is a tool for the distributed collaborative documents editing which offers a small set of services.

## Compilation
+ Run `mvn package` in the main folder
+ Will be produced two JAR package in the *target* folder:
   + *TURING-Client.jar*
   + *TURING-Server.jar*
   
### Environment and Dependencies
+ Maven (>= 3.6)
+ JDK >= (>= 1.8)
+ Argparse4j (>= 0.8.1) - [Official Page](https://argparse4j.github.io/)
+ JSON (20180813) - [Maven Repository](https://mvnrepository.com/artifact/org.json/json)

## Execution
+ Run `java -jar target/TURING-Server.jar` to execute the TURING Server
+ Run `java -jar target/TURING-Client.jar` to execute the TURING Client

## Connection Options
Both TURING Client and Server can be executed with default parameters or with custom ones.

The involved variables are:
+ **Client**
    + *TCP_PORT* - TCP port used for commands/responses transmission  
    + *UDP_PORT* - UDP port used for multicast chat messages transmission 
    + *RMI_PORT* - TCP port used for RMI method call execution
    + *DATA_DIR* - Directory to store on editing documents in
    + *SERVER_ADDRESS* - Server IPv4 address 
+ **Server**
    + *TCP_PORT*
    + *RMI_PORT*
    + *DATA_DIR* - Directory which hosts the server-side documents and the serialized databases (users and documents)

### Command Line
All these variable are available via command line, and it's possible to see their correct use just running:

+ `java -jar TURING-Client.jar -h`
+ `java -jar TURING-Server.jar -h`    

### Configuration File
It's possible to use a JSON configuration file to avoid to pass all the connection parameters through the command line. The constant variable names used into this are the same written above.
To pass the configuration file name to the program you need to use the *CONFIG_FILE* command line's argument.
 
**IMPORTANT**: Command line arguments overwrite the configuration file ones!

This is an example config file:

```
{
    "TCP_PORT": 1596,
    "DATA_DIR": "/var/tmp/IdontKnow",   
}
```

### Documentation
It's possible to find a **PDF report** which points out the main technical details of the project and the **Javadoc HTML**.

Both of those are attached to this repository under the **"doc" directory**.