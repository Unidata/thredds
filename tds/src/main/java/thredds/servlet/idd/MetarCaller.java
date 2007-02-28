package thredds.servlet.idd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import java.util.LinkedHashMap;
//import java.util.Iterator;
import java.util.HashMap;
import java.util.Calendar;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: rkambic
 * Date: Jan 24, 2007
 * Time: 4:14:54 PM
 * To change this template use File | Settings | File Templates.
 */


public class MetarCaller {

    public MetarQuery openQuery() {
        MetarQuery mq = new MetarQuery( "/local/ldm/data/pub/native/surface/metar", new PrintWriter( System.out, true ) );
        return mq;
    }

    public HashMap getLatest() {
        MetarQuery mq = new MetarQuery( "/local/ldm/data/pub/native/surface/metar", new PrintWriter( System.out, true ) );
        HashMap hm = mq.getLatest();
        //HashMap hm = mq.getTimeSeries( "KDEN" );
        //mq.getTimeSeries( "KSEA" );
        mq.close();
        return hm;
    }


    static public void main( String[] args ){

        MetarCaller mc = new MetarCaller();
        mc.getLatest();
        //mc.getTimeSeries( "KDEN" );

/*

        MetarQuery mq = new MetarQuery( "/local/ldm/data/pub/native/surface/metar" );
        //MetarQuery mq = new MetarQuery( );

            //pw.println("inside MetarQuery");
            System.out.println("System from MetarCaller");
            Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
            long end = cal.getTimeInMillis();
            long start = end - 3600000; // 60 minutes less
            //mq.getTimeRange(start, end);
            //mq.getLatest( start );
            //mq.getLatest();
            mq.getTimeSeries( "KDEN" );
            //mq.getTimeSeries( "KSEA" );
            mq.close();
            return;
*/
    }
}
