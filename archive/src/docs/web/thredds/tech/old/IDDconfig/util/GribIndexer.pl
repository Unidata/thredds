#!/usr/bin/perl
#
# Name:  	GribIndexer.pl
# 
# Author: 	Robb Kambic
# Date  : 	Mar 26, 2007
# 
# Purpose: 	walks directory sturcture making Grib Indexes as needed.
#    
# Description:  
#
# @param  startdir the directory where to start indexing
# @param  configuration file that has the above parameters on one line.
#
# process command line switches
while ($_ = $ARGV[0], /^-/) {
	 shift;
       last if /^--$/;
	      /^-D(.*)/ && ($debug = $1);
	     /^(-v)/ && $verbose++;
	     /^(-c)/ && ( $clearLocks = "clear" );
	     /^(-d)/ && ( $startDir = shift ) ;
	     /^(-f)/ && ( $conf = shift ) ;
	     /^(-j)/ && ( $JAVA = shift ) ;
	     /^(-t)/ && ( $tomcatHome = shift ) ;
}
# set configuration file GribIndexer.conf
if( ! defined( $conf ) ) {
	$conf = "/local/ldm/etc/GribIndexer.conf";
}

# configure Java and thredds environment
# set JAVA
if( ! defined( $JAVA ) ) {
	$JAVA = `which java`;
	chop( $JAVA );
}
if( $JAVA ) {
	$JAVA .= " -Xmx256m";
} else {
	print "java not defined/found\n";
	exit( 1 );
}
print "$JAVA\n";

# set tomcatHome
if( ! defined( $tomcatHome ) ) {
	$tomcatHome = "/opt/tomcat";
}
$tomcatHome .= "/webapps/thredds/WEB-INF";
print "$tomcatHome\n";

# figure out correct names/versions of jar files
$gribJar = `/bin/ls $tomcatHome/lib/grib*`;
chop( $gribJar );
$netcdfJar = `/bin/ls $tomcatHome/lib/netcdf*`;
chop( $netcdfJar );
$nlogJar = `/bin/ls $tomcatHome/lib/nlog*`;
chop( $nlogJar );

$ENV{ 'CLASSPATH' } = ":$gribJar:$netcdfJar:$tomcatHome/lib/jpeg2000.jar:$tomcatHome/classes:$tomcatHome/lib/jdom.jar:$nlogJar";
#
print "$ENV{ 'CLASSPATH' }\n";

# Index files now
print "`$JAVA ucar/grib/GribIndexer $clearLocks $conf`";
