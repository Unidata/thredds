/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/**
 * User: rkambic
 * Date: Nov 11, 2010
 * Time: 3:55:40 PM
 */

package thredds.server.radarServer;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.query.Station;
import thredds.server.config.TdsContext;
import thredds.servlet.HtmlWriter;
import thredds.servlet.UsageLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationRadarServerController extends AbstractController {

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private TdsContext tdsContext;
  private HtmlWriter htmlWriter;
  private boolean htmlView;

  /**
    * The view to forward to in case an dataset needs to be created.
  */
  private static final String CREATE_VIEW = "forward:createstation.htm";

  /**
  * The model key used to retrieve the message from the model.
  */
  private static final String MODEL_KEY = "message";

  /**
  * The unique key for retrieving the text associated with this message.
  */
  private static final String MSG_CODE = "message.create.station";


  public  StationRadarServerController() {}


  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public void setHtmlWriter( HtmlWriter htmlWriter ) {
    this.htmlWriter = htmlWriter;
  }

  public boolean isHtmlView()
  {
    return htmlView;
  }

  public void setHtmlView( boolean htmlView )
  {
    this.htmlView = htmlView;
  }

  @Override
  protected ModelAndView handleRequestInternal( HttpServletRequest request,
          HttpServletResponse response ) throws Exception
  {
    try
    {
      // Gather diagnostics for logging request.
      log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );
      // setup
      String pathInfo = request.getPathInfo();
      if (pathInfo == null) pathInfo = "";
      if ( ! pathInfo.endsWith( "stations.xml")) {
        log.info( "Invalid request "+ pathInfo );
        throw new RadarServerException( "Invalid request "+ pathInfo );
      }
      DatasetRepository.RadarType radarType = DatasetRepository.RadarType.nexrad;  //default
      try {
        String rt = pathInfo.substring(1, pathInfo.indexOf('/', 1));
        radarType = DatasetRepository.RadarType.valueOf( rt );
      } catch ( Exception e ) {
        log.info( "Invalid dataset url reference "+ pathInfo );
        throw new RadarServerException( "Invalid dataset url reference "+ pathInfo );
      }
      // return stations of dataset
      Map<String,Object> model = new HashMap<String,Object>();
      if (pathInfo.endsWith("stations.xml")) {
        pathInfo = pathInfo.replace("/stations.xml", "");
        stationsXML( radarType, pathInfo.substring(1), model );
      }
      if (model == null || model.size() == 0 ) {
         ModelAndView mav = new ModelAndView(CREATE_VIEW);
         mav.addObject(MODEL_KEY, MSG_CODE);
         return mav;
      } else {
        if ( this.htmlView )
        {
          //int i = HtmlWriter.getInstance().writeCatalog(request, response, station, true);
          //log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, i ) );
          return null;
        }
        else
          return new ModelAndView( "stationXml", model );
      }

    }
    catch ( RadarServerException e )
    {
      throw e; // pass it onto Spring exceptionResolver
    }
    catch ( Throwable e )
    {
      log.error( "handleRequestInternal(): Problem handling request.", e );
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ) );
      throw new RadarServerException( "handleRequestInternal(): Problem handling request." );
    }
  }
  /*
   * Create an ArrayList of station entries in model for radarType and path
   */
  private void stationsXML( DatasetRepository.RadarType radarType, String path, Map<String,Object> model )
    throws Exception {
      // stations in this dataset, set by path
      String[] stations = stationsDS( radarType, DatasetRepository.dataLocation.get(path ));
      if( path.contains( "level3") && stations[ 0 ].length() == 4 ) {
          for( int i = 0; i < stations.length; i++ )
               stations[ i ] = stations[ i ].substring( 1 );
      }
      makeStationDocument( stations,  radarType, model );
  }
  /*
   * Find the stations for this dataset in path directory
   */
  private String[] stationsDS( DatasetRepository.RadarType radarType, String path ) throws Exception {

    String[] stations = null;
    // Scan directory looking for actual stations
    if ( path != null ) {
      File dir = new File( path );
      stations = dir.list();
      // check for level3 stations
      if ( path.contains( "level3")) {
        dir = null;
        if ( radarType.equals( DatasetRepository.RadarType.nexrad)) {
          for( String var : stations) {
            if ( var.equals( "N0R") ) {
              dir = new File( path +"/N0R" );
              break;
            }
          }
        } else if (radarType.equals( DatasetRepository.RadarType.terminal ) ) {
          for( String var : stations) {
            if ( var.equals( "TR0" ) ) {
              dir = new File( path +"/TR0" );
              break;
            }
          }
        }
        if ( dir != null ) {
           stations = dir.list();
        } else {
          stations = null;
        }
      }
    }
    if( stations != null ) {
      // rescan stations array for removal of . files
      ArrayList<String> tmp = new ArrayList<String>();
      for( String station : stations ) {
        if( station.startsWith( "."))
          continue;
        tmp.add( station );
      }
      if( stations.length != tmp.size() ) {
        stations = new String[tmp.size()];
        stations = (String[]) tmp.toArray( stations );
      }
    }
    // no stations found return all known stations for RadarType
    if( stations ==  null || stations.length == 0 ) {
      if ( stations ==  null )
        stations = new String[ 1 ];
      if( radarType.equals( DatasetRepository.RadarType.nexrad ))
        stations =  DatasetRepository.nexradMap.keySet().toArray( stations );
      else
        stations =  DatasetRepository.terminalMap.keySet().toArray( stations );
    }
    return stations;
  }

  /**
  * Create  StationEntry objects in entries ArrayList
   * @param stations
  */
  private void makeStationDocument( String[] stations, DatasetRepository.RadarType radarType, Map<String,Object> model)
      throws Exception {
    /*
    <station id="KTYX" state="NY" country="US">
      <name>MONTAGUE/Fort_Drum</name>
      <longitude>-75.76</longitude>
      <latitude>43.76</latitude>
      <elevation>562.0</elevation>
    </station>
    */
    List<StationEntry> entries = new ArrayList<StationEntry>();
    for (String s : stations ) {
      Station stn = getStation( s, radarType );
      StationEntry se =  new StationEntry();
      if( stn == null ) { // stn not in table
        se.setId( s );
        se.setState( "XXX" );
        se.setCountry( "XX" );
        se.setName( "Unknown" );
        se.setLongitude( "0.0" );
        se.setLatitude( "0.0" );
        se.setElevation( "0.0" );
        continue;
      }
      // id
      se.setId( s );
      if( stn.getState() != null ) {
         se.setState( stn.getState() );
      }
      if( stn.getCountry() != null ) {
        se.setCountry( stn.getCountry() );
      }
      if (stn.getName() != null) {
        se.setName( stn.getName() );
      }
      se.setLongitude( ucar.unidata.util.Format.d(stn.getLocation().getLongitude(), 6) );
      se.setLatitude( ucar.unidata.util.Format.d(stn.getLocation().getLatitude(), 6) );
      if (!Double.isNaN(stn.getLocation().getElevation())) {
        se.setElevation( ucar.unidata.util.Format.d(stn.getLocation().getElevation(), 6) );
      }
      entries.add( se );
    }
    model.put( "stations", entries );
  }

  /**
   * returns station or null
   * @param station
   * @param radarType
   * @return  station
   */
  public Station getStation( String station, DatasetRepository.RadarType radarType ) {

    Station stn = null;
    if( station.length() == 3 && radarType.equals( DatasetRepository.RadarType.terminal ) ) { // terminal level3 station
      stn = DatasetRepository.terminalMap.get( "T"+ station );
    } else if( station.length() == 3 ) {
      for( Station stn3 : DatasetRepository.nexradList ) {
         if( stn3.getValue().endsWith( station ) ) {
           stn = stn3;
           break;
         }
      }
    } else if( radarType.equals( DatasetRepository.RadarType.terminal ) ) {
      stn = DatasetRepository.terminalMap.get( station );
    } else {
       stn = DatasetRepository.nexradMap.get( station );
    }
    return stn;
  }

  /*
    StationEntry provides the necessary information for a station entry below.

    <station id="KTYX" state="NY" country="US">
      <name>MONTAGUE/Fort_Drum</name>
      <longitude>-75.76</longitude>
      <latitude>43.76</latitude>
      <elevation>562.0</elevation>
    </station>
    */
  public class StationEntry {

    private String id;
    private String state;
    private String country;
    private String name;
    private String longitude;
    private String latitude;
    private String elevation;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getLongitude() {
      return longitude;
    }

    public void setLongitude(String longitude) {
      this.longitude = longitude;
    }

    public String getLatitude() {
      return latitude;
    }

    public void setLatitude(String latitude) {
      this.latitude = latitude;
    }

    public String getElevation() {
      return elevation;
    }

    public void setElevation(String elevation) {
      this.elevation = elevation;
    }
  }
}
