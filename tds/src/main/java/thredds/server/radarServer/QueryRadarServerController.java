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
 * Date: Oct 13, 2010
 * Time: 11:19:50 AM
 */

package thredds.server.radarServer;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.server.config.TdsContext;
import thredds.server.ncSubset.QueryParams;
import thredds.servlet.HtmlWriter;
import thredds.servlet.UsageLog;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Processes Queries for the RadarServer  Spring Framework
 */
public class QueryRadarServerController extends AbstractController  {

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private HtmlWriter htmlWriter;
  private boolean htmlView;
  private boolean releaseDataset = false;

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

    public boolean isReleaseDataset()
  {
    return releaseDataset;
  }

  public void setReleaseDataset( boolean releaseDataset )
  {
    this.releaseDataset = releaseDataset;
  }

  /**
    * The view to forward to in case a bad query.
    */
  private static final String CREATE_VIEW = "forward:badquery.htm";

   /**
    * The model key used to retrieve the message from the model.
   */
  private static final String MODEL_KEY = "message";

  /**
   * The unique key for retrieving the text associated with this message.

    */
  private static final String MSG_CODE = "message.bad.query";

  /*
   * why calculate over and over again  1970-01-01T00:00:00
   */
  private static DateType epicDateType;
  static {
    try {
         epicDateType = new DateType(RadarServerUtil.epic, null, null);
     } catch (java.text.ParseException e) {
     }
  }

  static SimpleDateFormat dateFormat;
  static {
    dateFormat = new SimpleDateFormat( "yyyyMMdd", Locale.US );
    dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
  }

  /**
   * Query RadarServer controller for Spring Framework
   * @param request HttpServletRequest
   * @param response HttpServletResponse
   * @return ModelAndView
   * @throws Exception
   */
  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    try
    {
      // Gather diagnostics for logging request.
      log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );
      // catch rogue invalid request here
      if ( request.getQueryString() == null ) {
        log.info( "Invalid dataset url reference "+ request.getPathInfo() );
        throw new RadarServerException( "Invalid dataset url reference "+ request.getPathInfo() );
      }
      // Query results in model
      Map<String,Object> model = new HashMap<String,Object>();
      radarQuery( request, response, model );
      if (model == null || model.size() == 0 ) {
         ModelAndView mav = new ModelAndView(CREATE_VIEW);
         mav.addObject(MODEL_KEY, MSG_CODE);
         return mav;
      } else {
        return new ModelAndView( "queryXml", model );
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

  // get/check/process query  
  public void radarQuery( HttpServletRequest req, HttpServletResponse res, Map<String,Object> model )
            throws ServletException, IOException, RadarServerException {

//      long  startms = System.currentTimeMillis();
//      long  endms;
      DatasetRepository.RadarType radarType = DatasetRepository.RadarType.nexrad;
      // need to extract data according to the (dataset) given
      String pathInfo = req.getPathInfo();
      if (pathInfo == null) pathInfo = "";
      if( pathInfo.startsWith( "/"))
              pathInfo = pathInfo.substring( 1 );
      try {
        String rt = pathInfo.substring(0, pathInfo.indexOf('/', 1));
        radarType = DatasetRepository.RadarType.valueOf( rt );
      } catch ( Exception e ) {
        log.info( "Invalid dataset url reference "+ pathInfo );
        throw new RadarServerException( "Invalid dataset url reference "+ pathInfo );
      }
      Boolean level2 = pathInfo.contains( "level2" );

      // parse the input
      QueryParams qp = new QueryParams();
      if( ! qp.parseQuery(req, res, new String[]{ QueryParams.XML, QueryParams.HTML, QueryParams.RAW, QueryParams.NETCDF})) {
        //log.error( "parseQuery Failed "+ qp.errs.toString() + req.getQueryString() );
        //throw new RadarServerException( qp.errs.toString() );//+ req.getQueryString() );
        return; //TODO: uncomment above 2 lines when QueryParams exception is fixed
      }
//      endms = System.currentTimeMillis();
//      System.out.println( "after QueryParams "+ (endms - startms));
//      startms = System.currentTimeMillis();
      // check Query Params
      if( ! checkQueryParms( radarType, qp, level2 ) ) {
        log.error( "checkQueryParms Failed "+ qp.errs.toString() + req.getQueryString() );
        throw new RadarServerException( qp.errs.toString() );//+ req.getQueryString() );
      }
//      endms = System.currentTimeMillis();
//      System.out.println( "after checkQueryParms "+ (endms - startms));
//      startms = System.currentTimeMillis();

      // check type of output wanted XML html
      qp.acceptType = qp.acceptType.replaceFirst( ".*/", "" );

      // creates first part of catalog
      if( ! createHeader( radarType,  qp, pathInfo, model ) ) {
        log.error( "Write Header Failed "+ qp.errs.toString() + req.getQueryString() );
        throw new RadarServerException( qp.errs.toString() ); // req.getQueryString() );
      }
//      endms = System.currentTimeMillis();
//      System.out.println( "after writeHeader "+ (endms - startms));
//      startms = System.currentTimeMillis();
      // gets products according to stations, time, and variables
      boolean dataFound = false;
      List<DatasetEntry> entries = new ArrayList<DatasetEntry>();
      if( qp.vars == null) {
        dataFound = processQuery( pathInfo, qp, null, entries  );
        if ( releaseDataset )
          DatasetRepository.removeRadarDatasetCollection( pathInfo, null );
      } else {
        int count = 0;
        for( String var : qp.vars ) {
          dataFound = processQuery( pathInfo, qp, var, entries  );
          if ( dataFound )
            count++;
          if ( releaseDataset )
            DatasetRepository.removeRadarDatasetCollection( pathInfo, var );
        }
        if ( count > 0 )
          dataFound = true;
      }
      // save entries
      model.put( "datasets", entries );
      if ( dataFound ) {
        model.put( "documentation", Integer.toString( entries.size()) +" datasets found for query");
      } else if( qp.errs.length() > 0){
        model.put( "documentation",  qp.errs.toString() );
      } else {
        model.put( "documentation", "No data available for station(s) and time range");
      }

//      endms = System.currentTimeMillis();
//      System.out.println( "after radarQuery "+ (endms - startms));
//      startms = System.currentTimeMillis();

  } // end radarNexradQuery

  // check that parms have valid stations, vars and times
  private Boolean checkQueryParms(DatasetRepository.RadarType radarType, QueryParams qp, Boolean level2 )
    throws IOException {
      if (qp.hasBB) {
        if( radarType.equals( DatasetRepository.RadarType.nexrad ) )
          qp.stns = RadarServerUtil.getStationNames(qp.getBB(), DatasetRepository.nexradList );
        else
          qp.stns = RadarServerUtil.getStationNames(qp.getBB(), DatasetRepository.terminalList );

        if (qp.stns.size() == 0) {
          qp.errs.append( "Bounding Box contains no stations " );
          return false;
        }
        if( ! level2 )
            qp.stns = RadarServerUtil.convert4to3stations( qp.stns );
      }

      if (qp.hasStns ) {
        if( RadarServerUtil.isStationListEmpty(qp.stns, radarType )) {
          qp.errs.append( "No valid stations specified, need 1 " );
          return false;
        } else if( level2 ) {
          for( String stn : qp.stns ) {
            if( stn.length() == 3 ) {
              qp.errs.append( "Need 4 character station names " );
              return false;
            }
          }
        } else if( ! level2 )
            qp.stns = RadarServerUtil.convert4to3stations( qp.stns );
      }

      if (qp.hasLatlonPoint) {
        qp.stns = new ArrayList<String>();
        if( radarType.equals( DatasetRepository.RadarType.nexrad ) )
          qp.stns.add( RadarServerUtil.findClosestStation(qp.lat, qp.lon, DatasetRepository.nexradList ));
        else
          qp.stns.add( RadarServerUtil.findClosestStation(qp.lat, qp.lon, DatasetRepository.terminalList ));
        if( ! level2 )
            qp.stns = RadarServerUtil.convert4to3stations( qp.stns );
      } else if (qp.fatal) {
        qp.errs.append( "No valid stations specified 2 " );
        return false;
      }

      if (qp.stns == null || qp.stns.size() == 0) {
        qp.errs.append( "No valid stations specified, need 1 " );
        return false;
      }
      boolean useAllStations = ( qp.stns.get( 0 ).toUpperCase().equals( "ALL"));
      if (useAllStations) {
        if( radarType.equals( DatasetRepository.RadarType.nexrad ) )
          qp.stns = RadarServerUtil.getStationNames( DatasetRepository.nexradList ); //need station names
        else
          qp.stns = RadarServerUtil.getStationNames( DatasetRepository.terminalList ); //need station names
        if( ! level2 )
            qp.stns = RadarServerUtil.convert4to3stations( qp.stns );
      }

      if( qp.hasTimePoint ) {
          if( qp.time.isPresent() ) {
              try {
                  qp.time_end = new DateType( "present", null, null);
                  qp.time_start = epicDateType;
              } catch (java.text.ParseException e) {
                  qp.errs.append("Illegal param= 'time' must be valid ISO Duration");
                  return false;
              }
          } else {
              qp.time_end = qp.time;
              qp.time_start = qp.time;
          }
      } else if( qp.hasDateRange ) {
          DateRange dr = qp.getDateRange();
          qp.time_start = dr.getStart();
          qp.time_end = dr.getEnd();
      } else { // get all times
         qp.time_latest = 1;
         //qp.hasDateRange = true;
         try {
             qp.time = new DateType( "present", null, null);
             qp.time_end = new DateType( "present", null, null);
             qp.time_start = epicDateType;
         } catch (java.text.ParseException e) {
             qp.errs.append("Illegal param= 'time' must be valid ISO Duration ");
             return false;
         }
      }

      if( level2 ) {
          qp.vars = null; // level2 can't select vars
      } else if( qp.vars == null ) { //level 3 with no vars
        qp.errs.append( "No vars selected ");
        return false;
      } else if( qp.vars.get( 0 ).contains( "/" )) { // remove desc from vars
            ArrayList<String> tmp = new ArrayList<String>();
            for ( String var:  qp.vars ) {
                tmp.add( var.replaceFirst( "/.*", "" ) );
            }
            qp.vars = tmp;
      }
    return true;
  }

  // create catalog Header
  private Boolean createHeader(DatasetRepository.RadarType radarType, QueryParams qp,
                    String pathInfo, Map<String, Object> model)
      throws IOException {

      Boolean level2 = pathInfo.contains( "level2");
      int level = (level2) ? 2 : 3;
      StringBuffer str = new StringBuffer();
      str.append("Radar Level").append( level ).append( " datasets in near real time" );
      model.put( "name", str.toString() );
      str.setLength( 0 );
      str.append("/thredds/dodsC/").append( pathInfo ).append("/");
      model.put( "base", str.toString() );
      str.setLength( 0 );
      str.append("RadarLevel").append( level ).append(" datasets for available stations and times");
      model.put( "dname", str.toString() );
      str.setLength( 0 );
        str.append( "accept=" ).append( qp.acceptType ).append( "&");
        if( ! level2 && qp.vars != null ) { // add vars
            str.append("var=");
            for (int i = 0; i < qp.vars.size(); i++ ) {
                str.append( qp.vars.get( i ) );
                if( i < qp.vars.size() -1 ) {
                    str.append( "," );
                }
            }
            str.append("&");
        }
        // use all stations
        if( qp.stns.get( 0 ).toUpperCase().equals( "ALL") ) {
            str.append("stn=ALL&");
        } else if (qp.hasStns ) {
            for (String station : qp.stns) {
                str.append("stn=").append( station ).append( "&");
            }
        } else if (qp.hasBB) {
            str.append("south=").append(qp.south).append( "&north=").append(qp.north).append("&" );
            str.append("west=").append( qp.west).append( "&east=").append(qp.east).append("&");
        }

        // no time given
        if( qp.time_latest == 1 ) {
          //str.deleteCharAt( str.length() -1);
          str.append("time=present");
        } else if (qp.hasDateRange ) {
          if( qp.time_start.getDate() == null || qp.time_start.isBlank() ||
            qp.time_end.getDate() == null || qp.time_end.isBlank() ) {
            str.append("time_start=").append( qp.time_start.toString());
            str.append( "&time_end=").append( qp.time_end.toString() );
            qp.errs.append( "need ISO time format " );
            return false;
          } else {
            str.append("time_start=").append( qp.time_start.toDateTimeStringISO());
            str.append( "&time_end=").append( qp.time_end.toDateTimeStringISO() );
          }
        } else if( qp.time.isPresent() ) {
            str.append("time=present");
        } else if( qp.hasTimePoint ) {
          if( qp.time.getDate() == null || qp.time.isBlank()) {
            str.append("time=").append( qp.time.toString() );
            qp.errs.append( "need ISO time format " );
            return false;
          } else {
            str.append( "time=").append( qp.time.toDateTimeStringISO() );
          }
        }
        model.put( "ID", str.toString() );

        if( level2 ) {
            model.put( "type", "NEXRAD2" );
        } else if( radarType.equals( DatasetRepository.RadarType.nexrad ) ){
            model.put( "type", "NIDS" );
        } else {
            model.put( "type", "TDWR" );
        }

      // at this point must have stations
      if( RadarServerUtil.isStationListEmpty(qp.stns, radarType )) {
        qp.errs.append( "No station(s) meet query criteria ");
        return false;
      }
    return true;
  }


/*
    Final Output format, save information in DatasetEntry de
     <dataset name="Level2_KFTG_20100121_0000.ar2v" ID="735519521"
        urlPath="KFTG/20100121/Level2_KFTG_20100121_0000.ar2v">
        <date type="start of ob">2010-01-21T00:00:00</date>
      </dataset>
*/
  private Boolean processQuery( String dataset, QueryParams qp,
    String var, List<DatasetEntry> entries ) throws RadarServerException {

    Boolean getAllTimes = true;
    String yyyymmddStart = null;
    String yyyymmddEnd = null;
    String dateStart = null;
    String dateEnd = null;
    try {
    if( ! qp.time_start.equals( epicDateType )) {
      getAllTimes = false;
      yyyymmddStart = qp.time_start.toDateString();
      yyyymmddStart = yyyymmddStart.replace( "-", "");
      yyyymmddEnd = qp.time_end.toDateString();
      yyyymmddEnd = yyyymmddEnd.replace( "-", "");
      dateStart =  yyyymmddStart +"_"+ RadarServerUtil.hhmm( qp.time_start.toDateTimeString() );
      dateEnd =  yyyymmddEnd +"_"+ RadarServerUtil.hhmm( qp.time_end.toDateTimeString() );
    }

    RadarDatasetCollection rdc = DatasetRepository.getRadarDatasetCollection( dataset, var );
    if ( rdc == null ) {
      qp.errs.append( "Invalid dataset =" ).append( dataset );
      qp.errs.append(" or var =").append( var );
      return false;
    }
    StringBuffer time = new StringBuffer();
    StringBuffer product = new StringBuffer();
    StringBuffer url = new StringBuffer();
    boolean isLevel2 = dataset.contains( "level2" );
    String type = ( isLevel2 ? "Level2" : "Level3");
    String suffix = ( isLevel2 ? ".ar2v" : ".nids");
    Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
    Date now =  cal.getTime();
    String currentDay = dateFormat.format( now );

    for ( String stn : qp.stns ) {
      RadarStationCollection rsc =  rdc.queryStation( stn, currentDay );
      if ( rsc == null)
        continue;
      for ( String day : rsc.getDays() ) {
        // check for valid day
        if( ! getAllTimes &&
            ! RadarServerUtil.isValidDay( day,  yyyymmddStart, yyyymmddEnd ) )
          continue;
        ArrayList<String> tal;
        if ( rdc.isCaseStudy() ) { //
          tal = rsc.getHourMinute( "all" );
          for ( String prod : tal ) {
            // check times
            if( ! getAllTimes &&
              ! RadarServerUtil.isValidDate( prod, dateStart, dateEnd ) )
              continue;
            // save this entry
            DatasetEntry de = new DatasetEntry();
            int idx = prod.indexOf( '/');
            if ( idx > 0 ) {
              de.setName( prod.substring( idx +1 ));
            } else {
              de.setName( prod  );
            }
            de.setID( Integer.toString( prod.hashCode() ));
            url.setLength( 0 );
            url.append( stn).append("/");
            if( var != null ) {
              url.append( var ).append( "/" );
            }
            url.append( prod );
            de.setUrlPath( url.toString() );
            de.setDate( RadarServerUtil.getObTimeISO( prod ) );
            entries.add( de );
          }
          continue;
        } else {
           tal = rsc.getHourMinute( day );
        }
        if (tal == null)
          continue;
        for ( String hm : tal ) {
          time.setLength( 0 );
          time.append( day ).append( "_" ).append( hm );
          if( ! getAllTimes &&
              ! RadarServerUtil.isValidDate( time.toString(), dateStart, dateEnd ) )
              continue;

          // save this entry
          DatasetEntry de = new DatasetEntry();
          
          product.setLength( 0 );
          product.append( type ).append( "_" ).append( rsc.getStnName() ).append( "_" );
          if( ! isLevel2 )
            product.append( var ).append( "_" );
          product.append( day ).append( "_" ).append( hm ).append( suffix );

          de.setName( product.toString() );
          de.setID( Integer.toString( product.toString().hashCode() ));
          url.setLength( 0 );
          if( ! isLevel2 ) {
            url.append( var ).append( "/" );
          }
          url.append(rsc.getStnName()).append("/").append(day).append("/").append(product.toString());
          de.setUrlPath( url.toString() );
          de.setDate( RadarServerUtil.getObTimeISO( product.toString() ) );
          entries.add( de );
          if( qp.hasTimePoint )
            break;
        }
        if( qp.hasTimePoint )
            break;
      }
    }
    return true;
    }
    catch ( Throwable e )
    {
      log.error( "Invalid dataset ="+ dataset +" or var ="+ var, e );
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ) );
      throw new RadarServerException( "Invalid dataset ="+ dataset +" or var ="+ var );
    }
  }

  /*
   * Used to store the information about a dataset
   */
  public class DatasetEntry {

    private String name;

    private String ID;

    private String urlPath;

    private String date;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getID() {
      return ID;
    }

    public void setID(String ID) {
      this.ID = ID;
    }

    public String getUrlPath() {
      return urlPath;
    }

    public void setUrlPath(String urlPath) {
      this.urlPath = urlPath;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }
  }

}
