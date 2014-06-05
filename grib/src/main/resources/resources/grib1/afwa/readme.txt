notes for tables in afwa subdir

  11/7/2011 afwa.tab has "Pressure" as param 131 but this conflicts with us of param no 1 in file Q:\cdmUnitTest\formats\grib1\us057g1010t04a000000000
   change to "Pressure AFWA"

 5/13/2013
  afwa.tab is coded for version 2. Now we have a file with table version 132.

7/31/2013
  (sean) afwa_133.tab is coded for version 133. This was obtained via John Raby, U.S. Army Research Lab, White Sands
  Missile Range, NM, when John requested support for the IDV (e-support ticket VVG-632378). John obtained the
  table from his AFWA point of contact Dan Rozema. A test file for version 133 can be found in the cdmUnitTest
  directory under formats/grib1/us057g1011t48b180000000.grb.
  afwa.tab is coded for version 2. Now we have a file with table version 132.

  (john) make center 57 (afwa) use AfwaTables for its  Grib1Customizer, for special levels handling

  (john) examine thredds\grib\src\main\sources\afwa\v133\AFWAGRIB 20130207.doc shows there are multiple subcenters (11).
  so change lookupTables.txt to use new table just for subcenter 1.

5/28/2014
  (lansing) moved af_2.tab, afwa.tab, and afwa_133.tab from local folder to separate afwa folder
