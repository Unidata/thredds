#!/usr/bin/perl
#
# Name:  	manageFileDirs.pl
# 
# Author: 	Robb Kambic
# Date  : 	Sept 2, 2005
# 
# Purpose: 	manage files in a directory sturcture.
#    
# Description:  
#
# @param  startdir the directory where to start managing
# @param  pattern  the pattern to match files or directories, yyyymmdd is a 
#	  place holder for year, month, day directories
# @param  number  the number of days to keep
#
# @param  configuration file that has the above parameters on one line.
#
# Once the script matches the pattern, it deletes the files or directories and
# then returns to the directory up one level. It skips files or directories
# that are links and prints out a message on files or directories that no
# action was taken on it. 
#
# process command line switches

while ($_ = $ARGV[0], /^-/) {
	 shift;
       last if /^--$/;
	      /^-D(.*)/ && ($debug = $1);
	     /^(-v)/ && $verbose++;
	     /^(-d)/ && ( $startDir = shift ) ;
	     /^(-p)/ && ( $pattern = shift ) ;
	     /^(-n)/ && ( $number = shift ) ;
	     /^(-f)/ && ( $conf = shift ) ;
}
# configuration file given to process
if( defined( $conf ) ) {
	print "Start ", `/bin/date`;
	open( CONF, "$conf" ) || die "cannot open $conf $!";
	while( <CONF> ) {
		next if( /^#/ );
		chop();
		( $startDir, $pattern, $number ) = split( /\s+/, $_ );
		next if( $pattern eq "" || $number eq "" );
		startChecking();
	}
	close CONF;
	print "End ", `/bin/date`;
} else {
	exit 1 if( $pattern eq "" || $number eq "" );
	print "Start ", `/bin/date`;
	startChecking();
	print "End ", `/bin/date`;
}

sub startChecking {

if( ! -d $startDir ) {
	print "$startDir doesn't exist\n";
	return;
}
chdir( $startDir );
if( $pattern =~ /yyyymmdd/i ) {
	$pattern = "^\\d{8}"; # pattern set to 8 digits
}
opendir( TOP, $startDir ) || die "cannot open $startDir $!";
( @TOP ) = readdir( TOP );
closedir( TOP );
checkDirs( @TOP );

} # end startChecking

# checkDirs is a recursive routine used to walk the directory structure in a
# depth first search for the designated pattern. Once the pattern is found, it 
# removes the files/directories in excess of the days to keep.  After deleting
# files or directories matching the pattern, the routine returns up one 
# directory level.
#
sub checkDirs{

my ( @INODES ) = @_;
local( $i, $j, $delete, @subINODES );

#print "currently in ", `pwd`;
for( $i = 0; $i <= $#INODES; $i++ ) {
	# skip ., .., and links
	next if( $INODES[ $i ] =~ /^\.$|^\.\.$/ || -l $INODES[ $i ] );
	if( $INODES[ $i ] =~ /$pattern/ ) {
		print `/bin/date`;
		print "currently in ", `pwd`;
		# just manage files/dirs matching pattern
		$delete = "";
		for( $j = 0; $j <= $#INODES; $j++ ) {
			next if( $INODES[ $j ] =~ /^\.$|^\.\.$/ || -l $INODES[ $j ] );
			next unless ( $INODES[ $j ] =~ /$pattern/ && -M $INODES[ $j ] > $number );
			$delete = $delete . " $INODES[ $j ]";
			# if grib file delete gbx index and inventory too
			if( $INODES[ $j ] =~ /grib.$/ ) {
				$delete = $delete . " $INODES[ $j ]" . ".gbx";
				$delete = $delete . " $INODES[ $j ]" . ".fmrInv.xml";
			}
			# if bufr file delete bfx index too
			if( $INODES[ $j ] =~ /bufr$/ ) {
				$delete = $delete . " $INODES[ $j ]" . ".bfx";
			}
		} 
		if( $delete ne "" ) {
			print "`/bin/rm -r $delete`\n";
			`/bin/rm -r $delete`;
		}
		#print "currently in ", `pwd`;
		return;
	} elsif( -d $INODES[ $i ] ) {
		opendir( DIR, $INODES[ $i ] ) || 
			die "cannot open $INODES[ $i ] $!";
		( @subINODES ) = readdir( DIR );
		closedir( DIR );
		chdir( $INODES[ $i ] );
		#print "currently in ", `pwd`;
		checkDirs( @subINODES );
		chdir( ".." );
		#print "currently in ", `pwd`;
	} elsif( $INODES[ $i ] =~ /\.scour/ ) {
		print "`/bin/rm $INODES[ $i ]`\n";
		`/bin/rm $INODES[ $i ]`;
	} else {
		print "currently in ", `pwd`;
		print "No action taken on $INODES[ $i ]\n";
	} 
}
} # end checkDirs
