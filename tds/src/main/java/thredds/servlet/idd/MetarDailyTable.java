/* Copyright (C) 2004 - 2006  db4objects Inc.  http://www.db4o.com

This file is part of the db4o open source object database.

db4o is free software; you can redistribute it and/or modify it under
the terms of version 2 of the GNU General Public License as published
by the Free Software Foundation and as clarified by db4objects' GPL 
interpretation policy, available at
http://www.db4o.com/about/company/legalpolicies/gplinterpretation/
Alternatively you can write to db4objects, Inc., 1900 S Norfolk Street,
Suite 350, San Mateo, CA 94403, USA.

db4o is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. */
package thredds.servlet.idd;

import java.util.*;
import java.io.PrintWriter;

import com.db4o.*;
import com.db4o.query.*;
import db4ounit.*;
import com.db4o.diagnostic.DiagnosticToConsole;


public class MetarDailyTable {
    
    static protected final String DBFILE = "MetarObs.db4o";
    static private ObjectContainer db = null;
    private PrintWriter pw;

    public MetarDailyTable(){
        new MetarDailyTable( null, new PrintWriter(System.out, true) );
    }
    
    public MetarDailyTable( String path, PrintWriter pw ){
        // open Db4o
        String DBMS;
        if( path != null ) { 
            DBMS = (( path.endsWith("/") )? path: path +"/") +DBFILE;
            pw.println( "DBMS ="+ DBMS );
        } else {
            DBMS = DBFILE;
        }
        if( db == null ) {
             //Db4o.configure().diagnostic().addListener(new DiagnosticToConsole());
             //Db4o.configure().messageLevel(3);
             //Db4o.configure().setOut( System.out );
             Db4o.configure().readOnly( true );
             Db4o.configure().objectClass(MetarObservation.class).objectField("timeObs").indexed(true);
             Db4o.configure().objectClass(MetarObservation.class).objectField("station").indexed(true);
             Db4o.configure().optimizeNativeQueries( true );
             Db4o.configure().automaticShutDown(false);
             db = Db4o.openFile( DBMS );
             Db4o.configure().readOnly( false );
        }
        this.pw = pw;
    }

    public void close() {
        db.close();
    }

    //public void makeDailyArchive(final long start,final long end) {
    public void makeDailyArchive( String path ) {
 
        // make archive database for 2 days ago
        Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        int year = cal.get( Calendar.YEAR );
        int month = cal.get( Calendar.MONTH );
        int day = cal.get( Calendar.DAY_OF_MONTH );
        //int hour = cal.get( Calendar.HOUR_OF_DAY );
        //System.out.println( "Year ="+ year +" Month ="+ month );
        // set day back 2, zero out hour minute sec
        cal.set( year, month, day -2, 0, 0, 0 );
        month = cal.get( Calendar.MONTH ) +1;
        String monthS;
        if( month < 10 ) {
            monthS = "0"+ Integer.toString( month );
        } else {
            monthS = Integer.toString( month );
        }
        day = cal.get( Calendar.DAY_OF_MONTH );
        String dayS;
        if( day < 10 ) {
            dayS = "0"+ Integer.toString( day );
        } else {
            dayS = Integer.toString( day );
        }

        String file = "Surface_Metar_"+ cal.get( Calendar.YEAR ) +
             monthS + dayS +".db4o";
        if( path != null ) { 
            file = (( path.endsWith("/") )? path: path +"/") +file;
        }
        pw.println( "file ="+ file );
            
        final long start = cal.getTimeInMillis() -90000; // -15 minutes 
        //System.out.println( "start ="+ cal.toString() );
        final long end = start +86400000; // + 24 hours

        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start && ob.timeObs < end;
            }
        });
        HashMap<String, MetarObservation> hm = removeDuplicates( list );

        // open database and store data for the day
        Db4o.configure().readOnly( false );
        Db4o.configure().objectClass(MetarObservation.class).objectField("timeObs").indexed(true);
        Db4o.configure().objectClass(MetarObservation.class).objectField("station").indexed(true);
        Db4o.configure().optimizeNativeQueries( true );
        Db4o.configure().automaticShutDown(false);
        ObjectContainer daily = Db4o.openFile( file );

        System.out.println( "HashMap size ="+ hm.size());
        for( Iterator it = hm.keySet().iterator(); it.hasNext();) {
            String key = (String)it.next();
            MetarObservation ob = (MetarObservation)hm.get( key );
            daily.set( ob );
            //System.out.println( ob.report );
        }
        daily.close();
        //printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return;
    }

    protected HashMap removeDuplicates( List<MetarObservation> list ) {
        HashMap<String, MetarObservation> hm = new HashMap( list.size() );
        for (MetarObservation ob : list){
            if (hm.containsKey( ob.key )) {
                int storedLength = (int)((MetarObservation) hm.get( ob.key )).length;
                if (storedLength < ob.length) { // replaced stored ob
                    MetarObservation newob = new MetarObservation( ob );
                    hm.put( ob.key, newob);
                }
             } else {
                MetarObservation newob = new MetarObservation( ob );
                hm.put( ob.key, newob);
            }
        }
        return hm;
    }

    public void printHashMap( HashMap<String, MetarObservation> hm ) {
        pw.println( "HashMap size ="+ hm.size());
        for( Iterator it = hm.keySet().iterator(); it.hasNext();) {
            String key = (String)it.next();
            MetarObservation ob = (MetarObservation)hm.get( key );
            pw.println( ob.report );
        }
    }

    public void printList( List<MetarObservation> list ) {
        pw.println( "list size ="+ list.size());
        for (MetarObservation ob : list){
            pw.println( ob.report );
        }
    }


    static public void main( String[] args ){

        MetarDailyTable mdt;
        if( args.length > 0 ) { 
            mdt = new MetarDailyTable( args[ 0 ], new PrintWriter(System.out, true) );
        } else {
            mdt = new MetarDailyTable();
        }

        Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        //cal.set( 2007, 0, 31, 22, 0, 0 );
        long end = cal.getTimeInMillis(); 
        //long start = end - 3600000; // 15 minutes less
        try {
            mdt.makeDailyArchive( args[ 0 ] );
        }
        finally {
            db.close();
        }
        Calendar now = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        System.out.println( "Took "+  (now.getTimeInMillis() - end) +" mill secs"); 
    }
}
