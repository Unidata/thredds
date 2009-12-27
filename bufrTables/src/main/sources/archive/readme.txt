This is an explanation of the naming convention of the BUFR tables and the
tablelookup.txt file used to designate Local tables and the latest WMO table.

For example, B4M-000-013-B  

The - is a section seperator

First section:
1st character B  : Stands for Bufr
2nd character 4  : Editon number; 2-4 Editions so far
3th  character M : This can be M for WMO table or L for a Local table

Second section:
1-3 characters    : Represents the Center ID; 000 is WMO, 059 is FSL

Third section:
1-3 characters    : Table version number Versions 0-14 so far

Fourth section:

1st character     : Table designator ie A, B, D

Fifth section: optional

Suffix .diff      : Local Table difference from lastest WMO table, ie
                    B3M-059-003-B.diff is the difference between FSL table
                    B4M-000-013-B and latest WMO table

Here's a sample configuration from tablelookup.txt

  59    B3L-059-003-ABD.diff

The 59 is the center id and B3L-059-003-ABD.diff is the Local table. Notice that
not one table is specified but 3 tables by using -ABD and this is a difference
between FSL table B3L-059-003-ABD and the latest WMO table.

---------------------------
Hand Correction (errors in the WMO docs) (10/26/2008):

B4M-000-013-B
0; 22; 39; 3; -5000; 12; m; Meteorological residual tidal elevation (surge or offset) (see Note 4)

B4M-000-013-D
3   2  44  (Evaporation data)
    0   4  24  Time period in hours
    0   2   4  Type of instrument for evaporation or crop type for evapotranspiration
    0  13  33  Evaporation /evapotranspiration
   -1


