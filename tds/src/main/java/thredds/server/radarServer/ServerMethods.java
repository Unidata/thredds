/* class ServerMethods
 *
 * Utility methods for all the THREDDS servlets
 * Outputs either xml, html, ascii, dqc data or catalog files.
 *
 * By:  Robb Kambic  08/12/2007
 *
 */

package thredds.server.radarServer;

import ucar.nc2.units.DateRange;
import thredds.catalog.query.*;

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

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import thredds.catalog.query.Station;
//import ucar.nc2.ncml.AggregationFmr;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

public class ServerMethods {

    public static final Pattern p_all_i = Pattern.compile("all", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_ascii_i = Pattern.compile("ascii", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_B_pound = Pattern.compile("^#");
    public static final Pattern p_catalog_i = Pattern.compile("catalog", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_config = Pattern.compile("(\\w+)\\s*=\\s+?(.*)");
    public static final Pattern p_dataset =
            Pattern.compile("ID=\"(.*)\"\\s+path=\"(.*)\"\\s+dirLocation=\"(.*)\"\\s+filter");
    public static final Pattern p_B_D8 = Pattern.compile("^\\d{8}");
    public static final Pattern p_DODS_i = Pattern.compile("(DODS|OPENDAP)", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_HTTPServer_i = Pattern.compile("HTTPServer", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_html_i = Pattern.compile("html", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_isodate = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");
    public static final Pattern p_latitude_longitude = Pattern.compile("latitude=\"([-.0-9]*)\"\\s+longitude=\"([-.0-9]*)\"");
    public static final Pattern p_name_value2 = Pattern.compile("name=\"([A-Za-z0-9 ()_,-:]*)\"\\s*");
    public static final Pattern p_nexrad2 =
            Pattern.compile("ID=\"(\\w+)/NEXRAD2\"\\s+path=\"(.*)\"\\s+dirLocation=\"(.*)\"\\s+filter");
    public static final Pattern p_qc_or_dqc_i = Pattern.compile("(qc|dqc)", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_space20 = Pattern.compile("%20");
    public static final Pattern p_spaces = Pattern.compile("\\s+");
    public static final Pattern p_station_name = Pattern.compile("\\s*<station\\s+name=\"(.*)\" ");
    public static final Pattern p_stn_i = Pattern.compile("stn", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_value2 = Pattern.compile("value=\"([A-Z0-9]*)\"");
    public static final Pattern p_xml_i = Pattern.compile("xml", Pattern.CASE_INSENSITIVE);
    public static final Pattern p_yyyymmdd_hhmm = Pattern.compile("(\\d{8}_\\d{4})");
    public static final String epic = "1970-01-01T00:00:00";

    static protected SimpleDateFormat dateFormatISO;
    static protected SimpleDateFormat dateFormat;

    static {
        dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormatISO.setTimeZone(TimeZone.getTimeZone("GMT")); // same as UTC
        dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); // same as UTC
    }

    //private ArrayList<Aggregation.Dataset> datasetList;
    //private Date start, end;

    private PrintWriter pw;
   /*
    protected String getPath() {
        return "idd/";
    }
    */
    /*
      * gets files in a directory that are directory themselves
      * returns them in descending order
    */
    public ArrayList getDirData(String dirS, PrintWriter pw) {

        File dir = new File(dirS);
        if( ! dir.exists() )
            return null;
        File[] dirs = dir.listFiles();
        ArrayList<File> onlyDirs = new ArrayList();

        // all entries must be directories
        for (int i = 0; i < dirs.length; i++) {
             if( dirs[ i ].isFile() )
                continue;
             onlyDirs.add(dirs[i]);
        }
        Collections.sort(onlyDirs, new CompareKeyDescend());

        return onlyDirs;

    }

    public String[] getDAYS(String dirS, PrintWriter pw) {

        // Obtain the days available
        // check for valid times
        //$check = `date -u +"%Y%m%d"` ;
        Date now = Calendar.getInstance().getTime();
        String check = dateFormat.format(now);

        File dir = new File(dirS);
        if( ! dir.exists() )
            return null;
        String[] TMP = dir.list();
        ArrayList days = new ArrayList();

        for (int i = 0; i < TMP.length; i++) {
            // check date starts with 8 numbers and not in the future
            if ( p_B_D8.matcher(TMP[i]).find() &&
                    TMP[i].compareTo(check) <= 0) {
                days.add(TMP[i]);
            }
        }
        Collections.sort(days, new CompareKeyDescend());
        String[] DAYS = new String[days.size()];
        DAYS = (String[]) days.toArray(DAYS);

        return DAYS;

    }


     ///////////////////////////////////////
  // station handling
  //static public List<Station> stationList = null;
 //private HashMap<String, Station> stationMap;

  /**
   * Determine if any of the given station names are actually in the dataset.
   *
   * @param stns List of station names
   * @return true if list is empty, ie no names are in the actual station list
   * @throws IOException if read error
   */
  public boolean isStationListEmpty(List<String> stns, HashMap<String, Station> map) {
    //HashMap<String, Station> map = getStationMap();
    for (String stn : stns) {
      if (map.get(stn) != null) return false;
    }
    return true;
  }
    /*
  public boolean isStationListEmpty(List<String> stns) throws IOException {
    HashMap<String, Station> map = getStationMap();
    for (String stn : stns) {
      if (map.get(stn) != null) return false;
    }
    return true;
  }
  */

  public boolean intersect(DateRange dr, Date start, Date end ) throws IOException {
    return dr.intersect(start, end);
  }

    /*
  public List<Station> getStationList() throws IOException {
    return stationList;
  }
   */


  public List<Station> getStations( String stnLocation ) {

      List <Station> stationList = new ArrayList();
      DocumentBuilder parser;
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      SelectStation parent = new SelectStation();  //kludge to use Station

      try {
          parser = factory.newDocumentBuilder();
          //stnLocation = stnLocation.replaceAll( " ", "%20");
          InputStream is = new FileInputStream( stnLocation );
          org.w3c.dom.Document doc = parser.parse(is);
          //System.out.println( "root=" + doc.getDocumentElement().getTagName() );
          NodeList stns = doc.getElementsByTagName("station");
          for (int i = 0; i < stns.getLength(); i++) {
              //System.out.println( "node=" + d.item( i ).getNodeName() );
              NamedNodeMap attr  = stns.item(i).getAttributes();
              String name = "", value = "", state = "", country = "" ;
              for (int j = 0; j < attr.getLength(); j++) {
                    if (attr.item(j).getNodeName().equals("value")) {
                        value = attr.item(j).getNodeValue();
                    } else if (attr.item(j).getNodeName().equals("name")) {
                        name = attr.item(j).getNodeValue();
                    } else if (attr.item(j).getNodeName().equals("state")) {
                        state = attr.item(j).getNodeValue();
                    } else if (attr.item(j).getNodeName().equals("country")) {
                        country = attr.item(j).getNodeValue();
                    }

              }
              NodeList child = stns.item(i).getChildNodes();  //Children of station
              Location location = null;
              for (int j = 0; j < child.getLength(); j++) {
                 //System.out.println( "child =" + child.item( j ).getNodeName() );
                 if ( child.item(j).getNodeName().equals("location3D")) {
                     NamedNodeMap ca  = child.item(j).getAttributes();
                     String latitude = "", longitude = "", elevation = "" ;
                     for (int k = 0; k < ca.getLength(); k++) {
                         if (ca.item(k).getNodeName().equals("latitude")) {
                             latitude = ca.item(k).getNodeValue();
                         } else if (ca.item(k).getNodeName().equals("longitude")) {
                             longitude = ca.item(k).getNodeValue();
                         } else if (ca.item(k).getNodeName().equals("elevation")) {
                             elevation = ca.item(k).getNodeValue();
                         }

                     }
                     location = new Location(latitude, longitude, elevation, null, null, null );
                 }
              }
              Station station = new Station( parent, name, value, state, country, null );
              station.setLocation( location );
              stationList.add( station );
          }

      } catch (SAXException e) {
          e.printStackTrace();
      } catch (IOException e) {
          e.printStackTrace();
      } catch (ParserConfigurationException e) {
          e.printStackTrace();
      }
      return stationList;
  }

  public List<Station> getStationsOld( String dqcLocation ) {

      StringBuffer errlog = new StringBuffer();
      try {

      DqcFactory dqcFactory = new DqcFactory(true);
      dqcLocation = dqcLocation.replaceAll( " ", "%20");    
      QueryCapability dqc = dqcFactory.readXML( dqcLocation );
      if (dqc.hasFatalError()) {
          errlog.append(dqc.getErrorMessages());
          return null;
      }

      // have a look at what selectors there are before proceeding
      SelectStation selStation = null;
      SelectList selTime    = null;
      SelectService selService = null;
      //SelectGeoRegion selRegion = null;

      ArrayList selectors = dqc.getSelectors();
      for (int i = 0; i < selectors.size(); i++) {
          Selector s = (Selector) selectors.get(i);
          if (s instanceof SelectStation) {
              selStation = (SelectStation) s;
              selStation.getStations();
          }
          if (s instanceof SelectList) {
              selTime = (SelectList) s;
          }
          if (s instanceof SelectService) {
              selService = (SelectService) s;
          }
          // if (s instanceof SelectGeoRegion)
          //   selRegion = (SelectGeoRegion) s;
      }
      //this.stationList = selStation.getStations();
      return selStation.getStations();

      } catch ( IOException ioe ){

      }
      //return stationList;
      return null;
  }
  public HashMap<String, Station> getStationMap( List<Station> list )  {
      //if (null == stationMap) {
        HashMap<String, Station> stationMap = new HashMap<String, Station>();
        //List<Station> list = getStationList();
        for (Station station : list) {
          stationMap.put(station.getValue(), station);
        }
      //}
      return stationMap;
    }

 /*
  private HashMap<String, Station> getStationMap() throws IOException {
    if (null == stationMap) {
      HashMap<String, Station> stationMap = new HashMap<String, Station>();
      List<Station> list = getStationList();
      for (Station station : list) {
        stationMap.put(station.getValue(), station);
      }
    }
    return stationMap;
  }
  */

  /**
   * Get the list of station names .
   *
   * @return list of station names
   * @throws IOException if read error
   */
  /*
  public List<String> getStationNames() throws IOException {
    ArrayList<String> result = new ArrayList<String>();
    List<Station> stations = getStationList();
    for (Station s : stations) {
        result.add(s.getValue());
    }
    return result;
  }
  */
  public List<String> getStationNames( List<Station> stations ) {
    ArrayList<String> result = new ArrayList<String>();
    //List<Station> stations = getStationList();
    for (Station s : stations) {
        result.add(s.getValue());
    }
    return result;
  }

  /**
   * Get the list of station names that are contained within the bounding box.
   *
   * @param boundingBox lat/lon bounding box
   * @return list of station names contained within the bounding box
   * @throws IOException if read error
   */
  /*
  public List<String> getStationNames(LatLonRect boundingBox) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList<String> result = new ArrayList<String>();
    List<Station> stations = getStationList();
    for (Station s : stations) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt)) {
        result.add(s.getValue());
        // boundingBox.contains(latlonPt);   debugging
      }
    }
    return result;
  }
  */
  public List<String> getStationNames(LatLonRect boundingBox, List<Station> stations ) {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList<String> result = new ArrayList<String>();
    //List<Station> stations = getStationList();
    for (Station s : stations) {
      latlonPt.set(s.getLocation().getLatitude(), s.getLocation().getLongitude());
      if (boundingBox.contains(latlonPt)) {
        result.add(s.getValue());
        // boundingBox.contains(latlonPt);   debugging
      }
    }
    return result;
  }

  /**
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat latitude value
   * @param lon longitude value
   * @return name of station closest to the specified point
   * @throws IOException if read error
   */
  /*
  public String findClosestStation(double lat, double lon) throws IOException {
    double cos = Math.cos(Math.toRadians(lat));
    List<Station> stations = getStationList();
    Station min_station = stations.get(0);
    double min_dist = Double.MAX_VALUE;

    for (Station s : stations) {
      double lat1 = s.getLatitude();
      double lon1 = LatLonPointImpl.lonNormal(s.getLongitude(), lon);
      double dy = Math.toRadians(lat - lat1);
      double dx = cos * Math.toRadians(lon - lon1);
      double dist = dy * dy + dx * dx;
      if (dist < min_dist) {
        min_dist = dist;
        min_station = s;
      }
    }
    return min_station.getValue();
  }
  */
  public String findClosestStation(double lat, double lon, List<Station> stations) {
    double cos = Math.cos(Math.toRadians(lat));
    //List<Station> stations = getStationList();
    Station min_station = stations.get(0);
    double min_dist = Double.MAX_VALUE;

    for (Station s : stations) {
      double lat1 = s.getLocation().getLatitude();
      double lon1 = LatLonPointImpl.lonNormal(s.getLocation().getLongitude(), lon);
      double dy = Math.toRadians(lat - lat1);
      double dx = cos * Math.toRadians(lon - lon1);
      double dist = dy * dy + dx * dx;
      if (dist < min_dist) {
        min_dist = dist;
        min_station = s;
      }
    }
    return min_station.getValue();
  }
  ////////////////////////////////////////////////////////////////
  // date filter
  /*
  private List<Aggregation.Dataset> filterDataset(DateRange range) {
    if (range == null)
      return datasetList;

    List<Aggregation.Dataset> result = new ArrayList<Aggregation.Dataset>();
    for (Aggregation.Dataset ds : datasetList) {
      if (range.intersect(ds.time_start, ds.time_end))
        result.add(ds);
    }
    return result;
  }

  Aggregation.Dataset filterDataset(DateType want) {
    if (want.isPresent())
      return datasetList.get(datasetList.size() - 1);

    Date time = want.getDate();
    for (Aggregation.Dataset ds : datasetList) {
      if (time.before(ds.time_end) && time.after(ds.time_start)) {
        return ds;
      }
      if (time.equals(ds.time_end) || time.equals(ds.time_start)) {
        return ds;
      }
    }
    return null;
  }

   */
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
            s = ServerMethods.class.getResourceAsStream(filename);
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
                String encodedUrl =  p_space20.matcher(filename).replaceAll(" ");
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
    public boolean isValidDate( String dateReport, String dateStart, String dateEnd ) {

       Matcher m;
       m = p_yyyymmdd_hhmm.matcher( dateReport );
       String date;
       if( m.find() ) {
           date = m.group( 1 );
       } else {
           return false;
       }
       // extract hhmm from product 
       if( date.compareTo( dateStart ) >= 0 &&
           date.compareTo( dateEnd ) <= 0 )
           return true;

       return false;

    }
 
    // returns hhmm of datetime string
    public String hhmm( String dateTime ) {
       return dateTime.substring( 11, 13 ) + dateTime.substring( 14, 16 );
    }

    // returns ISO time extracted from a product 
    public String getObTimeISO( String product ) {
       Matcher m;
       m = p_yyyymmdd_hhmm.matcher( product );
       String date;
       if( m.find() ) {
           date = m.group( 1 );
           return date.substring(0,4) +"-"+ date.substring(4,6)
           +"-"+ date.substring(6,8) +"T"+ date.substring(9, 11)
           +":"+ date.substring(11,13) +":00";
       } else {
           return "0000-00-00T00:00:00";
       }
    }

    public void setPW( PrintWriter pw ){
        this.pw = pw;
    }
} // end ServerMethods
