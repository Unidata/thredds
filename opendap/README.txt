
$Id: README 22423 2010-03-30 16:59:55Z dmh $


This directory contains the JPL/URI/OSU port of the DODS Data Access Protocol
(DAP) to Java. Documentation for DODS can be found on the DODS home page at
http://unidata.ucar.edu/packages/dods/.

**NOTE: The test suites have been repaired. The test server (dods.servers.test.dts)
has been reworked and now produces stable output. There are no tests in the 
sdds-testsuite that fail currently.

The INSTALL file contains information on building DODS from source, building
the Javadoc API documentation, testing the library, and using it in other
Java programs.

Once you've built the software, test out the Java version of the DODS command
line tool geturl:

	java dods.util.geturl.Geturl <options> <dods url>

There is also a bourne shell (sh) script in dods/util/geturl called geturl
which should run geturl in a more tipy fashion.


See the DODS Web page for a list of valid URLs to datasets.

The DESIGN file describes differences in design between the original C++
version of the DAP, James Gallagher's initial Java design work, and Jake
Hamby's initial implementation. It also describes areas in which the library
could be optimized, how to extend the library to support future data types
and transport protocols, and any "gotcha's" discovered when working with
Java. It should be useful reading for anyone who wants to work with the DAP
source code to fix bugs or add new features.

