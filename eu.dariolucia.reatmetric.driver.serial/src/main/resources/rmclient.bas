'Reatmetric Powerbasic Client for
'serial driver.

'allocate 1KB buffer for serial
$COM 1024

'there is no wait or sleep in PB
'so use active wait
SUB wait (cycles%) SHARED
    FOR n = 1 TO cycles%
        temp = SQR(123.543)
    NEXT n
END SUB

SUB exithdlr SHARED
    'send BYE
    PRINT #1,"BYE"
    'read response line
    LINE INPUT #1,lastline$
    'close
    CLOSE 1
    connected% = 0
END SUB

SUB msghdlr SHARED
    CLS
    'set max log number
    PRINT #1,"SET_MAX_LOG 07"
    'read answer, ignore
    LINE INPUT #1,lastline2$

    'set msg text max size
    PRINT #1,"SET_LOG_LEN 26"
    'read answer, ignore
    LINE INPUT #1,lastline2$

    'start rendering
    'until user presses any key
    WHILE NOT INSTAT  'while no keypress
        endresponse% = 0
        'send request
        PRINT #1,"UPDATE_LOG"
        'read server response (and ignore)
        LINE INPUT #1,lastline2$
        WHILE endresponse% = 0
            'read server response
            LINE INPUT #1,lastline2$
            partofstr$ = MID$(lastline2$,1,2)
            IF partofstr$ = "OK" THEN
                'done
                endresponse% = 1
            ELSE
                'print to screen
                PRINT lastline2$
            END IF
        WEND 'end of response
        'wait some time and repeat
        waittime% = 200
        CALL wait(waittime%)
    WEND  'loop until key is pressed
END SUB

SUB paramhdlr SHARED
    CLS
    'first deregister params if any
    PRINT #1,"DEREG_PARAM_ALL"
    'read answer, ignore
    LINE INPUT #1,lastline2$
    'ask for file to load
    LINE INPUT "Provide AND file name to load: ",lastline$
    'open file indicated by user
    OPEN lastline$ FOR INPUT AS #2
    'read one line and send to server
    WHILE NOT EOF(2)
        'read line
        LINE INPUT #2,lastline$
        IF LEN(lastline$) > 0 THEN
            'send line to server
            PRINT #1,lastline$
            'read server response line
            LINE INPUT #1,lastline2$
            IF MID$(lastline2$,1,2) = "OK" THEN
                'registration ok, next
            ELSE
                'registration fail
                PRINT "Registration failed: ";
                PRINT lastline$
                waittime% = 10
                CALL wait(waittime%)
                'go on
            END IF
        END IF
    WEND
    CLOSE #2

    'if here, set value length
    PRINT #1,"SET_VALUE_LEN 22"
    'read answer, ignore
    LINE INPUT #1,lastline2$

    'if here, then start rendering
    'until user presses any key
    WHILE NOT INSTAT  'while no keypress
        endresponse% = 0
        'send request
        PRINT #1,"UPDATE_PARAM"
        'read server response (and ignore)
        LINE INPUT #1,lastline2$
        'clean screen
        CLS
        WHILE endresponse% = 0
            'read server response
            LINE INPUT #1,lastline2$
            IF MID$(lastline2$,1,2) = "OK" THEN
                'done
                endresponse% = 1
            ELSE
                'print to screen
                PRINT lastline2$
            END IF
        WEND 'end of response
        'wait some time and repeat (200 is ca. 3 seconds)
        waittime% = 200
        CALL wait(waittime%)
    WEND  'loop until key is pressed
END SUB

SUB processinput SHARED
inputcycle:
    CLS
    PRINT "1: AND"
    PRINT "2: Messages"
    PRINT "3: Exit"
    INPUT "Selection: ",userinput%

    IF userinput% = 1 THEN
        'parameters
        CALL paramhdlr
        GOTO inputcycle
    ELSEIF userinput% = 2 THEN
        'messages
        CALL msghdlr
        GOTO inputcycle
    ELSEIF userinput% = 3 THEN
        'exit
        CALL exithdlr
    ELSE
        CLS
        PRINT "Wrong selection"
        TONE 7,50
        waittime% = 10
        CALL wait(waittime%)
        GOTO inputcycle
    END IF
END SUB

'startup: it tries to
'open the connection to the system

CLS
TONE 10,50
TONE 15,50
TONE 17,50
PRINT "Reatmetric Client"
PRINT "Connecting..."
PRINT

attemps% = 5
connected% = 0
nattempt% = 1
lastline$ = ""

WHILE nattemp% <= attemps%
    CLS
    PRINT "Reatmetric Client"
    PRINT "Connecting... Attempt ";nattempt%;"\";attemps%;
    PRINT
    waittime% = 10
    CALL wait(waittime%)
    'open communications port at 4800 baud
    'using even parity, 7 data bits,
    '1 stop bit, suppress RTS, ignore
    'CTS and DSR (and CD by default)
    'appends LF to every CR character
    OPEN "com1:4800,e,7,1,rs,cs,ds,lf" AS 1

    'send HELLO Atari Portfolio
    PRINT #1,"HELLO Atari Portfolio"
    'read response line
    LINE INPUT #1,lastline$

    'if response is HI RTM, good
    IF lastline$ = "HI RTM" THEN
        TONE 10,50
        connected% = 1
        nattemp% = attemps% + 1
    ELSE
        TONE 7,50
        nattemp% = nattemp% + 1
    END IF
WEND

IF connected% = 1 THEN
    CLS
    PRINT "Reatmetric Client"
    PRINT "Connected to remote system"
    PRINT
    WHILE connected% = 1
        CALL processinput
    WEND
    CLS
    PRINT "Reatmetric Client"
    PRINT "Have a nice day"
    PRINT
ELSE
    CLS
    PRINT "Reatmetric Client"
    PRINT "Cannot connect... Exiting"
    PRINT
END IF
CLOSE
