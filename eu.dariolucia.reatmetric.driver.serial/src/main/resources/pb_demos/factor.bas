'Factorial calculator

$STACK 32000
DEF FNfactorial#(i%)
  total# = total# * i%
  IF i% > 1 THEN _
    subb# = FNfactorial#(i% - 1)
  FNfactorial# = total#
END DEF

CLS
total# = 1
PRINT "Input the number you wish to calculate"
INPUT "the factorial of: ",j%
PRINT "Factorial =";FNfactorial#(j%)