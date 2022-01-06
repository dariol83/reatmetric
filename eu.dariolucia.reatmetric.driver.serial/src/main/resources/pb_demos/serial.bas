'This program displays all characters
'received at the serial port on the
'screen and sends all characters you
'type on the keyboard to the serial
'port (it does not echo the characters
'you type on the screen however).

'establish large comm buffer
$COM 10000

CLS

'open communications port at 9600 baud
'using even parity, 7 data bits,
'1 stop bit, suppress RTS, ignore
'CTS and DSR (and CD by default)
OPEN "com1:9600,e,7,1,rs,cs,ds" AS 1

waitformore:
WHILE NOT INSTAT  'while no keypress
x%=LOC(1)   'get # of chars waiting
IF x%>0 THEN  'if characters waiting,
  PRINT INPUT$(x%,1);  'get & display
END IF
WEND  'loop until key is pressed
a$=INKEY$  'get key pressed
PRINT #1,a$;  'send it to comm port
GOTO waitformore  'repeat cycle forever
CLOSE