## Serial Driver
This driver allows sending real-time monitoring data (currently parameters and log message) to devices connected 
to a specified serial port.

### Protocol
This protocol has been designed to allow visualisation of ReatMetric real-time data using very low power/low 
resources terminals, such as the Atari Portfolio™ (1989) via serial or parallel ports, or basic serial terminals. 
It is not supposed to be used for fully fledged data transfers. Use the available remoting connector for that, 
or write your own driver to match your needs.

The protocol is fully synchronous, with the knowledge of the session kept on the server side (i.e. on this driver). 
Interactions are always started by the client. The server always waits for client instructions. All messages are 
US-ASCII encoded. The client **must** wait for the server to reply to its command and complete the response, before 
sending the next command.

Server-side, the protocol has two main states:

*DEREGISTERED*: all messages received when the server is in this state are rejected with message ABORT\r 
and the server remains in the DEREGISTERED state, except if the HELLO message is received. An HELLO message 
transitions the server into the REGISTERED state.

*REGISTERED*: HELLO messages received when the server is in this state are rejected with message ABORT\r 
and transitions the server into the DEREGISTERED state. A BYE message transitions the server into the DEREGISTERED 
state.

In all states, unrecognised messages send back an ABORT\r and the server transitions in the DEREGISTERED state,
if not there already.

#### Registration

client > server

    HELLO <name>\r

server > client

    HI RTM\r
    
#### Deregistration

client > server

	BYE\r

server > client

	CYA\r

#### Keep-Alive

The server expects to receive a message from the connected and registered client every X seconds. If no message is 
received, the server assumes that the client is disconnected. This is a server side feature: compatible servers must be 
able to reply to a PING, but they might not have a connection timeout implemented.

client > server

	PING\r

server > client

	PONG\r

#### Set length of value strings in parameter update

client > server

	SET_VALUE_LEN <length of value strings>\r

server > client (if OK)

	OK\r
	
server > client (if not OK, i.e. wrong number/wrong value)
    
   	KO\r

#### Register monitoring parameter

client > server

	REG_PARAM <param name>\r

server > client (if OK)

	OK <param #, 2 digits number>\r

server > client (if NOK, i.e. parameter not found)

	KO\r

The protocol allows a maximum of 99 parameters to be registered at a given point in time.

#### Deregister monitoring parameter

client > server

	DEREG_PARAM <param #, 2 digits number>\r

server > client (if OK)

	OK\r

server > client (if NOK, i.e. parameter number not existing)

	KO\r

#### Deregister all monitoring parameters

client > server

	DEREG_PARAM_ALL\r

server > client

	OK\r

#### Request update of registered parameters

client > server

	UPDATE_PARAM\r

server > client

	<# records, 2 digits number>\r
	# records lines, each formatted as: 
	<param #, 2 digits> <hh:mm:ss> <value as string> <V|I|E|U|D> <ALM|WRN|VIO|NOM|N/A|N/C|ERR|UNK|IGN>\r
	OK\r

If no parameters are registered, the answer will be:

    00\r
    OK\r
	
Example (assuming that the value length is set to 10)

	04\r
	01 08:12:33   56.42234 V NOM
    02 08:12:32   2256.422 V WRN
    03 08:11:58 Testing ac V NOM
    04 08:12:08          0 I N/A
    OK\r

Note that the full length of the variable block (the list of parameters) can be derived as soon as the number of entries is known.

#### Set max number of log messages in log update

client > server

	SET_MAX_LOG <# max log events, 2 digits number>\r

server > client (if OK)

	OK\r

server > client (if NOK, i.e. number too large)

	KO\r

The protocol allows a maximum number of 99 log messages to be delivered in a single go.

#### Set length of message strings in log update

client > server

	SET_LOG_LEN <length of message strings, 2 digits number>\r

server > client (if OK)

	OK\r

#### Request update of log updates

client > server

	UPDATE_LOG\r

server > client

	<# records, 2 digits number>\r
	# records lines, each formatted as: 
	<hh:mm:ss> <ALM|WRN|INF|ERR|UNK> <message>\r
	OK\r

If no new logs are raised from the previous call, the answer will be:

    00\r
    OK\r

Note that the full length of the variable part becomes known as soon as the number of entries is known.
