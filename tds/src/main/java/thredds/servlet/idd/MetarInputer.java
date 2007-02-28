package thredds.servlet.ldm;

/**
 * Created by IntelliJ IDEA.
 * User: rkambic
 * Date: Jan 25, 2007
 * Time: 2:43:58 PM
 * To change this template use File | Settings | File Templates.
 */

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
//import java.io.EOFException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.*;


public class MetarInputer {

    //static protected final String DBMS = "MetarObs.db4o";
    static final Pattern CtrlAC = Pattern.compile("(\\x01|\\x03)");


    static public void main( String[] args ){

    // accessDb4o
    String DBMS = "MetarObs.db4o";
    if( args.length > 0 ) { 
        DBMS = (( args[ 0 ].endsWith("/") )? args[ 0 ]: args[ 0 ] +"/") +DBMS;
        System.out.println( "DBMS ="+ DBMS );
    }
    //new File(DBMS).delete();
    //Configuration conf = Db4o.configure();
    Db4o.configure().readOnly( false );
    Db4o.configure().flushFileBuffers( true );
    Db4o.configure().singleThreadedClient( true );
    Db4o.configure().objectClass(MetarObservation.class).objectField("timeObs").indexed(true);
    Db4o.configure().objectClass(MetarObservation.class).objectField("station").indexed(true);
    ObjectContainer db = Db4o.openFile(DBMS);
    try {
        // do something with db4o
        storeMetars(db);
    }
    finally {
        db.close();
    }

    }

    public static void storeMetars(ObjectContainer db) {

        //, report = "KDEN 262315 25035KT";
        StringBuilder report = new StringBuilder();
        InputStreamReader in = new InputStreamReader( System.in );
        BufferedReader bin = new BufferedReader( in );

        try {
            boolean endOfOb = false;
            String input = "";
            MetarObservation metar;
            int count = 0; // used to commit ever 100 obs
            int delete = 0; // used to remove old data ever 1000 obs
            while( (input = bin.readLine()) != null ) {
                if (input.length() < 25 ) {
                    continue;
                }
                // get report
                do {
                    if( input.length() < 5 &&  CtrlAC.matcher(input).find()) {
                        endOfOb = true; 
                    } else {
                        report.append( input );
                    }
                    if( report.indexOf( "=", report.length() -5 ) > 0 ||
                        report.indexOf( "$", report.length() -5 ) > 0 ||
                        endOfOb ) { 
                        //System.out.println("report ="+ report );
                        metar = new MetarObservation();
                        if( metar.decode( report.toString() ) ) {
                            db.set(metar);
                            count++;
                            //System.out.println( metar.toString() );
                            //System.out.println( report.toString() );
                        }
                        report.setLength( 0 );
                        endOfOb = false;
                        if( count == 100 ) {
                            db.commit();
                            count = 0;
                            // remove obs over 3 days old on hourly basis
                            if( ++delete == 80 ) { // ~8000 reports / hour
                                long remove = metar.timeObs -259200000;
                                //long remove = metar.timeObs -3600000;
                                removeBeforeTime( db, remove );
                                delete = 0;
                            }
                        }
                        break;
                    }
                    report.append( " " );
                } while( (input = bin.readLine()) != null );
                //System.out.println("input ="+ input +" length ="+ input.length());
                //if( count > 8000 ) System.out.println("count ="+ count );
            } // end while
            bin.close();

        } catch ( IOException ioe ) {
            //break;
            System.out.println("IOException caught");
        }
    } // end storeMetars

    public static void removeBeforeTime( ObjectContainer db, final long start ) {

        ObjectSet<MetarObservation> os = db.query(new Predicate <MetarObservation> () {
            public boolean match(MetarObservation ob){
                return ob.timeObs < start;
            }
        });
        while( os.hasNext() ) {
            MetarObservation ob = os.next();
            //System.out.println("deleting "+ ob.report );
            db.delete( ob );
        }
        db.commit();
        return;
    }

}
