'Musical guessing game

SUB higher SHARED
'called when guess too low

'play "guess higher" tune (C,D,E)
TONE 10,50
TONE 12,50
TONE 14,50

PRINT "Too low -- guess again!"

END SUB


SUB lower SHARED
'called when guess too high

'play "guess lower" tune (E,D,C)
TONE 14,50
TONE 12,50
TONE 10,50

PRINT "Too high -- guess again!"

END SUB


SUB correct SHARED
'called when guess correct

'play "got it" tune (C,C,E,G,E,G)
TONE 10,50
TONE 10,50
TONE 14,50
TONE 17,50
TONE 14,50
TONE 17,50

PRINT
PRINT "Congratulations!  You got it in"
PRINT "only";n%;"guesses!"
done% = 1

END SUB


SUB startup SHARED
'called when starting up

CLS
PRINT "Welcome to the musical guessing"
PRINT "game!  I am guessing a number"
PRINT "between 1 and 100..."
PRINT

'seed random number generator
RANDOMIZE (VAL(MID$(TIME$,4,2)) * _
   VAL(RIGHT$(TIME$,2)))

'play "startup" tune: a random
'number of random notes

FOR t% = 1 TO int(rnd*5)+5
  TONE INT(RND*25)+1, 50
NEXT    '5..9 random 0.5-second notes

num% = INT(RND*100)+1   'pick number
done% = 0               'not done yet
n% = 0                  '# of tries

END SUB


SUB closing SHARED
'called to wind up game

PRINT "Thanks for playing!"

'play "closing" tune (C2,F,F,A,F,B,C2)
TONE 0,50
TONE 22,50
TONE 15,50
TONE 15,50
TONE 19,50
TONE 15,50
TONE 21,50
TONE 22,50

END SUB


CALL startup    'main start
WHILE done% = 0
  INPUT "What is your guess";g%
  n% = n% + 1
  IF g% > num% THEN
    CALL lower  'too high
  ELSEIF g% < num% THEN
    CALL higher 'too low
  ELSE
    CALL correct  'got it
  END IF
WEND
CALL closing    'done
END
