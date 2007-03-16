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


public class MetarQuery {
    
    static protected final String DBFILE = "MetarObs.db4o";
    //static protected final String DBFILE = "Surface_Metar_20070225.db4o";
    static private ObjectContainer db = null;
    private PrintWriter pw;

    public MetarQuery( String path, PrintWriter pw ){
        // open Db4o
        String DBMS;
        if( path != null ) { 
            DBMS = (( path.endsWith("/") )? path: path +"/") +DBFILE;
            //pw.println( "DBMS ="+ DBMS );
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

    public ArrayList<MetarObservation> getTimeSeries(final String stn) {
 
        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.station.equals( stn );
            }
        }, new MetarCompareKeyDescend() );

        ArrayList<MetarObservation> al = new ArrayList( ((int)list.size() / 2));
        String key = "";
        int position = 0;
        int lastLen = 0;
        for ( ListIterator li = list.listIterator(); li.hasNext(); ){
            MetarObservation mo = (MetarObservation) li.next();
            if( mo.key.equals( key )) {
                if( mo.length > lastLen ) {
                    al.set( position -1, new MetarObservation( mo ));
                    lastLen = mo.length;
                }
            } else {
                al.add( new MetarObservation( mo ));
                key = mo.key;
                lastLen = mo.length;
                position++;
            }
            //pw.println( ob.report );
        }
        //printList( list );
        //pw.println( "------" );
        printList( al );
        return al;
    }

    public ArrayList<String> getStations() {
 
        ArrayList station = new ArrayList( 6000 );
        HashMap<String, MetarObservation> hm = getLatest();
        pw.println( "HashMap size ="+ hm.size());
        for( Iterator it = hm.keySet().iterator(); it.hasNext();) {
            // key = wwww ddhhmm
            String key = (String)it.next();
            //pw.println( key );
            String stn = key.substring( 0, 5 );
            if( ! station.contains( stn ) )
                station.add( stn ); 
        }
        printStation( station );
        return station;
    }

    public ArrayList<String> getStations( final long start, final long end ) {
 
        ArrayList station = new ArrayList( 6000 );
        HashMap<String, MetarObservation> hm = getTimeRange(start, end) ;
        //pw.println( "HashMap size ="+ hm.size());
        for( Iterator it = hm.keySet().iterator(); it.hasNext();) {
            // key = wwww ddhhmm
            String key = (String)it.next();
            //pw.println( key );
            String stn = key.substring( 0, 5 );
            if( ! station.contains( stn ) )
                station.add( stn ); 
        }
        printStation( station );
        return station;
    }

    public HashMap<String, MetarObservation> getLatest() {
        Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        final long start = cal.getTimeInMillis() -3600000; 
 
        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start;
                //return ob.timeObs > 1170867940767L;
            }
        });
        HashMap<String, MetarObservation> hm = removeDuplicates( list );
        printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return hm;
    }

    public HashMap<String, MetarObservation> getLatest( String[] station ) {
        Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        final long start = cal.getTimeInMillis() -3600000; 
 
        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start;
            }
        });
        HashMap<String, MetarObservation> hm = boundingBox( list, station );
        //HashMap<String, MetarObservation> hm = removeDuplicates( list );
        printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return hm;
    }

        public HashMap<String, MetarObservation> getFromTime( final long start ) {

        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start;
            }
        });
        HashMap<String, MetarObservation> hm = removeDuplicates( list );
        printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return hm;
    }

    public HashMap<String, MetarObservation> getFromTime( final long start, String[] station ) {

        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start;
            }
        });
        HashMap<String, MetarObservation> hm = boundingBox( list, station );
        printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return hm;
    }


    public HashMap<String, MetarObservation> getTimeRange(final long start,final long end) {
 
        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start && ob.timeObs < end;
            }
        });
        HashMap<String, MetarObservation> hm = removeDuplicates( list );
        printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return hm;
    }

    public HashMap<String, MetarObservation> getTimeRange(final long start,final long end, String[] station ) {
 
        List<MetarObservation> list = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs > start && ob.timeObs < end;
            }
        });
        HashMap<String, MetarObservation> hm = boundingBox( list, station );
        printHashMap( hm );
        //pw.println( "-----------------------------------------------");
        //printList( list );
        return hm;
    }

    public void printList( List<MetarObservation> list ) {
        //pw.println( "list size ="+ list.size());
        for (MetarObservation ob : list){
            //pw.print( ob.timeObs +"  "+ ob.dateISO +"  " );
            pw.println( ob.report );
        }
    }

    public void printStation( ArrayList<String> list ) {
        //pw.println( "list size ="+ list.size());
        for (String stn : list){
            pw.println( stn );
        }
    }

    public void printHashMap( HashMap<String, MetarObservation> hm ) {
        //pw.println( "HashMap size ="+ hm.size());
        for( Iterator it = hm.keySet().iterator(); it.hasNext();) {
            String key = (String)it.next();
            MetarObservation ob = (MetarObservation)hm.get( key );
            pw.println( ob.report );
        }
    }

    private HashMap<String, MetarObservation> boundingBox( List<MetarObservation> list, String[] station ) {
        HashMap<String, MetarObservation> hm = new HashMap( 1000 );
        HashSet<String> hs =  new HashSet( station.length );
        for( int i = 0; i < station.length; i++ )
            hs.add( station[ i ] );

        for (MetarObservation ob : list){
            if( ! hs.contains( ob.station ) )
                continue;

            if (hm.containsKey( ob.key )) {
                int storedLength = (int)((MetarObservation)hm.get( ob.key )).length;
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

    private class MetarCompareKeyDescend implements Comparator {

        public int compare(Object o1, Object o2) {
            MetarObservation mo1 = (MetarObservation) o1;
            MetarObservation mo2 = (MetarObservation) o2;
            //String s1 = (String) mo1.key;
            //String s2 = (String) mo2.key;

            //return s2.compareTo(s1);
            return (int)( mo2.timeObs - mo1.timeObs );
        }
    }


    static public void main( String[] args ){

        MetarQuery mq;
        if( args.length > 0 ) { 
            mq = new MetarQuery( args[ 0 ], new PrintWriter(System.out, true) );
        } else {
            mq = new MetarQuery( null, new PrintWriter(System.out, true) );
        }

        Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        //cal.set( 2007, 0, 31, 22, 0, 0 );
        long end = cal.getTimeInMillis(); 
        long start = end - 3600000; // 15 minutes less
        String station[] = { "KDEN", "KSEA" };
        try {
        // do something with db4o
            //mq.getStations();
            //mq.getStations(start, end);
            mq.getFromTime( start );
            //mq.getFromTime( start, station );
            //mq.getLatest();
            //mq.getLatest( station ); // with bounding box
            //mq.getTimeSeries("KDEN");
            //mq.getTimeSeries("KSEA");
            //mq.getTimeRange(start, end);
            //mq.getTimeRange(start, end, station); // with bounding box
        }
        finally {
            db.close();
        }
        Calendar now = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        System.out.println( "Took "+  (now.getTimeInMillis() - end) +" mill secs"); 
    }
}
