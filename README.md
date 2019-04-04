# TURING
TURING (aka _dis**T**rib**U**ted collabo**R**ative ed**I**ti**NG**_) is a tool for the distributed collaborative documents editing which offers a small set of services.

## Compilation
+ Run `mvn package` in the main folder
+ Will be produced two JAR package in the *target* folder:
   + *TURING-Client.jar*
   + *TURING-Server.jar*
    
## Execution
+ Run `java -jar target/TURING-Server.jar` to execute the TURING Server
+ Run `java -jar target/TURING-Client.jar` to execute the TURING Client

### Connection Options
Each TURING Client and Server can be executed with default parameters or with custom ones.

The involved variables are:
+ **Client**
    + *TCP_PORT* - TCP port used for commands/responses transmission  
    + *UDP_PORT* - UDP port used for multicast chat messages transmission 
    + *RMI_PORT* - TCP port used for RMI method call execution
    + *DATA_DIR* - Directory to store documents in