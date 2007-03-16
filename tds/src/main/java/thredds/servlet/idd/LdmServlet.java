/* class LdmServlet
 *
 * Library for the Ldm Servlet
 *
 * Utility methods for all the THREDDS LDM servlets
 * Outputs either xml, html, ascii, dqc data or catalog files.
 *
 * By:  Robb Kambic  03/25/2005
 *
 */

package thredds.servlet.idd;

import thredds.servlet.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;

public class LdmServlet extends AbstractServlet {

    static protected ThreddsServerPatterns p = new ThreddsServerPatterns();
    static protected SimpleDateFormat dateFormatISO;
    static protected SimpleDateFormat dateFormat;

    static {
        dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormatISO.setTimeZone(TimeZone.getTimeZone("GMT")); // same as UTC
        dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); // same as UTC
    }

    public void init() throws ServletException {
        super.init();
    }

    protected void makeDebugActions() {
    }

    protected String getPath() {
        return "idd/";
    }

    public String[] getDAYS(String dirS, PrintWriter pw) {

        // Obtain the days available
        // check for valid times
        //$check = `date -u +"%Y%m%d"` ;
        Date now = Calendar.getInstance().getTime();
        String check = dateFormat.format(now);
        //pw.println( "<p>check = "+ check +"</p>");

        File dir = new File(dirS);
        if( ! dir.exists() )
            return null;
        String[] TMP = dir.list();
        ArrayList days = new ArrayList();

        for (int i = 0; i < TMP.length; i++) {
            // check date starts with 8 numbers and not in the future
            if (p.p_B_D8.matcher(TMP[i]).find() &&
                    TMP[i].compareTo(check) <= 0) {
                days.add(TMP[i]);
                //pw.println("<p>days[ " + i +" ] = "+ days.get( i ) +"</p>");
            }
        }
        Collections.sort(days, new CompareKeyDescend());
        //pw.println("list =" + list );
        String[] DAYS = new String[days.size()];
        DAYS = (String[]) days.toArray(DAYS);
        //for( int i = 0; i < DAYS.length; i++ )
           //pw.println("DAYS[ " + i + " ] =" + DAYS[ i ] );

        return DAYS;

    }

    public String[] boundingBox(String y0, String y1, String x0, String x1, 
        String dqc, PrintWriter pw) throws FileNotFoundException, IOException {

        ArrayList STNSal = new ArrayList();
        float lat, lon, minLat, minLon, maxLat, maxLon;
        Matcher m;
        boolean inBound;

        minLat = Float.parseFloat( y0 );
        maxLat = Float.parseFloat( y1 );

        minLon = Float.parseFloat( x0 );
        maxLon = Float.parseFloat( x1 );

        //pw.println( "<p>minLat="+ minLat +" minLon="+ minLon +" maxLat="+ maxLat +" maxLon="+ maxLon +"</p>" );
        BufferedReader br = getInputStreamReader(dqc);
        String input = "";
        String stn = "";
        String stnLine = "";

        while ((input = br.readLine()) != null) {
            if (!p.p_station_name.matcher(input).find()) {
               if( pw != null )
                  pw.println( input );
               continue;
            }
//		s#value="([A-Z0-9]*)"## ;
            m = p.p_value2.matcher(input);
            if (m.find()) {
                stnLine = input;
                stn = m.group(1);
            } else {
                continue;
            }
            input = br.readLine();
//		s#latitude="([-.[0-9]*)"\s+longitude="([-.[0-9]*)"## ;
            m = p.p_latitude_longitude.matcher(input);
            if (m.find()) {
                lat = Float.parseFloat(m.group(1));
                lon = Float.parseFloat(m.group(2));
            } else {
                continue;
            }

            //System.out.println( "<p>lat="+ lat +" lon="+ lon +" </p>" );
            inBound = true;
            if (lat < minLat || lat > maxLat) {
                inBound = false;
            }
            if (lon < minLon || lon > maxLon) {
                inBound = false;
            }
            if (inBound) {
                if( pw != null ) {
                    pw.println( stnLine );
                    pw.println( input );
                    input = br.readLine(); 

                    if( input.contains( "</station>" ) ) {
                        pw.println( input );
                    } else { // else read was elevation
                        pw.println( input );
                        input = br.readLine(); // </station> tag
                        pw.println( input );
                    }
                }
                if( stn.length() == 3 ) {
                    if( stnLine.lastIndexOf( " US") > 0 ) {
                        stn = "K" + stn;
                    } else if( stnLine.lastIndexOf( " CN") > 0 ) {
                        stn = "C" + stn;
                    } else {
                        stn = "K" + stn;
                    }
                }
                STNSal.add(stn);
                //System.out.println(  "<p>stn inbound "+ stn +" </p>" );
            } else {
                input = br.readLine(); 
                if( input.contains( "</station>" ) )
                    continue;  // else read was elevation
                input = br.readLine(); // </station> tag
            }
        }  // end while

        br.close();
        STNSal.trimToSize();
        //pw.println(  "STNSal.size()="+ STNSal.size() );
        String[] stns = new String[STNSal.size()];
        for (int i = 0; i < STNSal.size(); i++)
            stns[i] = (String) STNSal.get(i);

        //System.out.println(  "stns.length="+ stns.length );
        return stns;
    } // end boundingBox

    /**
     * Get an input stream reader for the filename
     *
     * @param filename name of file
     * @return corresponding input stream reader
     * @throws FileNotFoundException couldn't find the file
     * @throws IOException           problem opening stream
     */
    public static BufferedReader getInputStreamReader(String filename)
            throws FileNotFoundException, IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(filename)));
    }

    /**
     * Get an input stream for the filename
     *
     * @param filename name of file
     * @return corresponding input stream
     * @throws FileNotFoundException couldn't find the file
     * @throws IOException           problem opening stream
     */
    public static InputStream getInputStream(String filename)
            throws FileNotFoundException, IOException {
        return getInputStream(filename, null);
    }

    /**
     * Get an input stream for the filename
     *
     * @param filename name of file
     * @param origin   relative origin point for file location
     * @return corresponding input stream
     * @throws FileNotFoundException couldn't find the file
     * @throws IOException           problem opening stream
     */
    public static InputStream getInputStream(String filename, Class origin)
            throws FileNotFoundException, IOException {
        InputStream s = null;
        while (origin != null) {
            s = origin.getResourceAsStream(filename);
            if (s != null) {
                break;
            }
            origin = origin.getSuperclass();
        }
        //Try an absolute resource path
        if (s == null) {
            s = LdmServlet.class.getResourceAsStream(filename);
        }

        //Try the file system
        if (s == null) {
            File f = new File(filename);
            if (f.exists()) {
                try {
                    s = new FileInputStream(f);
                    //System.out.println( "opened file " + filename );
                } catch (Exception e) {
                }
            }
        }

        //Try it as a url
        if (s == null) {
            try {
                //Pattern p_space20 = Pattern.compile( "%20" );
                String encodedUrl = p.p_space20.matcher(filename).replaceAll(" ");
                URL dataUrl = new URL(encodedUrl);
                URLConnection connection = dataUrl.openConnection();
                s = connection.getInputStream();
            } catch (Exception exc) {
            }
        }
        if (s == null) {
            throw new FileNotFoundException("Unable to open:" + filename);
        }
        return s;
    } // end getInputStream

    protected class CompareKeyDescend implements Comparator {

        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;

            return s2.compareTo(s1);
        }
    }

    // returns if day is between dayStart and dayEnd
    public boolean isValidDay( String dateDir, String yyyymmddStart, String yyyymmddEnd )
    {

       if( dateDir.compareTo( yyyymmddStart ) >= 0 && 
           dateDir.compareTo( yyyymmddEnd ) <= 0 )
           return true;

       return false;

    }

    // returns if date is between dateStart and dateEnd
    public boolean isValidDate( String dateReport, String dateStart, 
       String dateEnd ) {

       if( dateReport.compareTo( dateStart ) >= 0 &&
           dateReport.compareTo( dateEnd ) <= 0 )
           return true;

       return false;

    }

} // end LdmServlet
