
proc map {s from to} {
    set len [string length $from]
    while {1} {
	set idx [string first  $from $s]
	if {$idx<0} {break}
	set s "[string range $s 0 [expr {$idx-1}]]$to[string range $s [incr idx $len] end]"
    }
    set s
}


proc replace {template args} {
    foreach {key value} $args {
	set template [map $template  $key $value ]
    }
    return $template
}




proc  projToLatLon {prefix body} {
    set ::projToLatLon(prefix) $prefix
    set ::projToLatLon(body) $body
}

proc  latLonToProj  {prefix body} {
    set ::latLonToProj(prefix) $prefix
    set ::latLonToProj(body) $body
}



set singleTemplate {
    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj (LatLonPoint latLon, ProjectionPointImpl result) {
        double toX, toY;
        double fromLat = latLon.getLatitude ();
	double fromLon = latLon.getLongitude ();
        %latLonToProj.prefix%	
        %latLonToProj.body%	
	result.setLocation (toX, toY);
	return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
	double toLat, toLon;
	double fromX = world.getX ();
	double fromY = world.getY ();
        %projToLatLon.prefix%	
        %projToLatLon.body%	
	result.setLatitude (toLat);
	result.setLongitude (toLon);
	return result;
    }
}

set loopTemplate {
    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates, 
     *                 where to[0][i], to[1][i] is the (x,y) coordinate 
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */
    public %type%[][] latLonToProj(%type%[][] from, %type%[][] to, int latIndex, int lonIndex) {
	int cnt = from[0].length;
	%type% []fromLatA = from[latIndex];
	%type% []fromLonA = from[lonIndex];
	%type% []resultXA = to[INDEX_X];
	%type% []resultYA = to[INDEX_Y];
        double toX, toY;
        %latLonToProj.prefix%
	for (int i=0; i<cnt; i++) {
	   double fromLat = fromLatA[i];
	   double fromLon = fromLonA[i];
           %latLonToProj.body%
           resultXA[i] = (%type%)toX;
           resultYA[i] = (%type%)toY;
	}
	return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public %type%[][] projToLatLon (%type%[][]from, %type%[][]to) {
	int cnt = from[0].length;
	%type% []fromXA = from[INDEX_X];
	%type% []fromYA = from[INDEX_Y];
	%type% []toLatA = to[INDEX_LAT];
	%type% []toLonA = to[INDEX_LON];
        %projToLatLon.prefix%				 
        double toLat, toLon;
	for (int i=0;i<cnt;i++) {
	      double fromX = fromXA[i];
              double fromY = fromYA[i];
              %projToLatLon.body%				 
              toLatA[i]= (%type%)toLat;
              toLonA[i]= (%type%)toLon;
	}
	return to;
    }
}


proc doSub {template type} {
    foreach a {latLonToProj projToLatLon} {
	global $a
	foreach n [array names $a] {
	    set template [replace $template  %$a.$n%   [set ${a}($n)] ]
	}
    }
    set template [replace $template  %type%  $type ]
    return $template
}



proc  process {file} {
    global latLonToProj projToLatLon singleTemplate loopTemplate
    catch {unset latLonToProj}
    catch {unset projToLatLon}

    set fp [open $file r]
    set c [read $fp]
    close $fp

    if {![regexp {/\*MACROBODY(.*)MACROBODY\*/} $c match body]} {
	puts "No MACROBODY  defined in $file"
	return
    }


    eval $body

    set code ""
    append code [doSub $singleTemplate {}]
    append code [doSub $loopTemplate float]
    append code [doSub $loopTemplate double]

    set result "/*BEGINGENERATED*/\n 
/*
Note this section has been generated using the convert.tcl script.
This script, run as: 
tcl convert.tcl [file tail $file]
takes the actual projection conversion code defined in the MACROBODY 
section above and generates the following 6 methods
*/\n
$code
"

set i1 [string first "/*BEGINGENERATED*/" $c]
set i2 [string first "/*ENDGENERATED*/" $c]
incr i2 -1
set c [string replace $c $i1 $i2 $result]

    set fp [open $file w]
    puts $fp $c
    close $fp
}


foreach file $argv {
	process $file
}

