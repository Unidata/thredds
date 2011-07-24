B0000000000098000000.TXT

Bufr Tables B and D are used to collect all necessary information to pack/unpack Bufr data. Which table is to
be loaded is decided at runtime using information from Section 1 of the Bufr message. The naming convention
for Bufr binary tables is as follows:

 Bssswwwwwxxxxxyyyzzz.TXT 
 Cssswwwwwxxxxxyyyzzz.TXT 
 Dssswwwwwxxxxxyyyzzz.TXT 

where

  sss - Master table number (zero for WMO meteorological tables)
  wwwww - Originating sub-centre
  xxxxx - Originating centre
  yyy - Version number of master table used
  zzz - Version number of local table used
  
ECMWF is currently using 

  B0000000000098013001.TXT
  C0000000000098013001.TXT 
  D0000000000098013001.TXT
  
tables. Keep in mind that Bufr Table C in this software is a code table. 
If standard WMO tables are used, the Originating centre xxxxx will be set to 00000 .

in:
m    sub   cent  ver local
B000 00000 00098 000 000.TXT
B000 00000 00098 002 001.TXT
B000 00000 00098 006 000.TXT
B000 00000 00098 006 001.TXT
B000 00000 00098 013 001.TXT
B000 00000 00254 011 001.TXT

C000 00000 00098 013 001.TXT

D000 00000 00098 000 000.TXT
D000 00000 00098 002 001.TXT
D000 00000 00098 006 000.TXT
D000 00000 00098 006 001.TXT
D000 00000 00098 013 001.TXT
D000 00000 00254 011 001.TXT

L000 00000 00098 012 001.TXT

where 
 98  = ECMWF
 254 = EUMETSAT Operation Centre