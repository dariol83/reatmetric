'Disk file translator

SUB translate(infile$,outfile$) SHARED
OPEN infile$ FOR INPUT AS #1
OPEN outfile$ FOR OUTPUT AS #2
PRINT "Translating..."

WHILE NOT EOF(1)
  LINE INPUT #1, a$     'get line
  a$ = UCASE$(a$)       'upper case
  b$ = ""

  'change all "&" to "AND"
  IF LEN(a$) > 0 THEN
    amp% = INSTR(a$,"&")
    DO WHILE amp% <> 0
      b$ = b$+LEFT$(a$,amp%-1)+"AND"
      a$ = RIGHT$(a$,LEN(a$)-amp%)
      amp% = INSTR(a$,"&")
    LOOP
    b$ = b$+a$
  END IF

  'now change all "W/O" to "WITHOUT"
  a$ = b$
  b$ = ""
  IF LEN(a$) > 0 THEN
    amp% = INSTR(a$,"W/O")
    DO WHILE amp% <> 0
      b$ = b$+LEFT$(a$,amp%-1)+"WITHOUT"
      a$ = RIGHT$(a$,LEN(a$)-(amp%+2))
      amp% = INSTR(a$,"W/O")
    LOOP
    b$ = b$+a$
  END IF

  'now change all "W/" to "WITH "
  a$ = b$
  b$ = ""
  IF LEN(a$) > 0 THEN
    amp% = INSTR(a$,"W/")
    DO WHILE amp% <> 0
      b$ = b$+LEFT$(a$,amp%-1)+"WITH "
      a$ = RIGHT$(a$,LEN(a$)-(amp%+1))
      amp% = INSTR(a$,"W/")
    LOOP
    b$ = b$+a$
  END IF

  PRINT #2, b$          'write it
WEND

CLOSE
PRINT "Done."

END SUB


CLS
LINE INPUT _
  "Name of file to translate: ";f$
LINE INPUT _
  "Name of new file to create: ";n$
CALL translate(f$,n$)
END
