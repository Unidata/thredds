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
 * Time: 11:19:08 AM
 */

package thredds.server.radarServer;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.*;
import thredds.server.config.TdsContext;
import thredds.servlet.HtmlWriter;
import thredds.servlet.UsageLog;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
//import java.io.PrintWriter;
import java.util.*;

public class CatalogRadarServerController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private HtmlWriter htmlWriter;

  private boolean htmlView;

  /**
    * The view to forward to in case an dataset needs to be created.
  */
  private static final String CREATE_VIEW = "forward:createdataset.htm";

  /**
  * The model key used to retrieve the message from the model.
  */
  private static final String MODEL_KEY = "message";

  /**
  * The unique key for retrieving the text associated with this message.
  */
  private static final String MSG_CODE = "message.create.dataset";


  public  CatalogRadarServerController() {}
  

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
      if( pathInfo.startsWith( "/"))
              pathInfo = pathInfo.substring( 1 );
      DatasetRepository.RadarType radarType = DatasetRepository.RadarType.nexrad;
      try {
        int idx = pathInfo.indexOf('/', 1);
        if ( idx > -1 ) {
          String rt = pathInfo.substring(0, pathInfo.indexOf('/', 1));
          radarType = DatasetRepository.RadarType.valueOf( rt );
        }  
      } catch ( Exception e ) {
        log.info( "Invalid dataset url reference "+ pathInfo );
        throw new RadarServerException( "Invalid dataset url reference "+ pathInfo );
      }
      // default is the complete radarCollection catalog
      InvCatalogImpl catalog = DatasetRepository.cat;
      if (pathInfo.startsWith("dataset.xml") || pathInfo.startsWith("catalog.xml")) {
      // level2 and level3 catalog/dataset
      } else if (pathInfo.contains("level2/catalog.") || pathInfo.contains("level3/catalog.")
          || pathInfo.contains("level2/dataset.") || pathInfo.contains("level3/dataset.")) {
        catalog = level2level3catalog( radarType, catalog, pathInfo );
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

      // return specific dataset information, ie IDD
      } else if (pathInfo.endsWith("dataset.xml") || pathInfo.endsWith("catalog.xml")) {
        Map<String,Object> model = datasetInfoXml(radarType, catalog, response, pathInfo);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
        if ( model != null )
          return new ModelAndView( "datasetXml", model );
        else
          return null;
      }
      if ( catalog == null  ) {
         ModelAndView mav = new ModelAndView(CREATE_VIEW);
         mav.addObject(MODEL_KEY, MSG_CODE);
         return mav;
      } else {
        if ( this.htmlView )
        {
          int i = HtmlWriter.getInstance().writeCatalog(request, response, catalog, true);
          log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, i ) );
          return null;
        }
        else
         return new ModelAndView( "threddsInvCatXmlView", "catalog", catalog );
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
      if ( ! response.isCommitted() ) response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
      return null;
    }
  }
  private InvCatalogImpl level2level3catalog( DatasetRepository.RadarType radarType, InvCatalogImpl cat, String pathInfo )
            throws RadarServerException, IOException {

    InvCatalogImpl tCat = null;
    try {
      /*
      StringBuffer aDs = new StringBuffer( pathInfo.substring(1) );
      int i = aDs.indexOf( "/catalog");
      if ( i > 0)
          aDs.delete( i, aDs.length());
      i = aDs.indexOf( "/dataset");
      if ( i > 0)
          aDs.delete( i, aDs.length());
      */

      String type;
      if (pathInfo.contains("level2"))
        type = radarType.toString() + "/level2";
      else
        type = radarType.toString() + "/level3";

      ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
      InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false);
      factory.writeXML(cat, os, true);
      tCat = factory.readXML(
          new ByteArrayInputStream(os.toByteArray()), DatasetRepository.catURI);
      /*
      Iterator parents = tCat.getDatasets().iterator();
      while (parents.hasNext()) {
        ArrayList<InvDatasetImpl> delete = new ArrayList<InvDatasetImpl>();
        InvDatasetImpl top = (InvDatasetImpl) parents.next();
        Iterator tDatasets = top.getDatasets().iterator();
        while (tDatasets.hasNext()) {
          InvDatasetImpl ds = (InvDatasetImpl) tDatasets.next();
          if (ds instanceof InvDatasetScan) {
            InvDatasetScan ids = (InvDatasetScan) ds;
            if (ids.getPath() == null)
              continue;
            if (ids.getPath().contains(aDs)) {
              ids.setXlinkHref(ids.getPath() + "/dataset.xml");
            } else {
              delete.add(ds);
            }
          }
        }
        // remove datasets
        for (InvDatasetImpl idi : delete) {
          top.removeDataset(idi);
        }
      }
      */
      Iterator parents = tCat.getDatasets().iterator();
      while (parents.hasNext()) {
        ArrayList<InvDatasetImpl> delete = new ArrayList<InvDatasetImpl>();
        InvDatasetImpl top = (InvDatasetImpl) parents.next();
        Iterator tDatasets = top.getDatasets().iterator();
        while (tDatasets.hasNext()) {
          InvDatasetImpl ds = (InvDatasetImpl) tDatasets.next();
          if (ds instanceof InvDatasetScan) {
            InvDatasetScan ids = (InvDatasetScan) ds;
            if (ids.getPath() == null)
              continue;
            if (ids.getPath().contains(type)) {
              ids.setXlinkHref(ids.getPath() + "/dataset.xml");
            } else {
              delete.add(ds);
            }
          }
        }
        // remove datasets
        for (InvDatasetImpl idi : delete) {
          top.removeDataset(idi);
        }
      }
    } catch (Throwable e) {
      log.error("RadarServer.level2level3catalog", "Invalid url request" );
      throw new RadarServerException( "Invalid catalog request" );
    }
    return tCat;
  }

  //private InvCatalogImpl datasetInfoXml(DatasetRepository.RadarType radarType, InvCatalogImpl cat, String pathInfo )
  private Map<String,Object> datasetInfoXml(DatasetRepository.RadarType radarType, InvCatalogImpl cat, HttpServletResponse res, String pathInfo )
      throws RadarServerException, IOException {

    // dataset results in model
    Map<String,Object> model = new HashMap<String,Object>();

    //PrintWriter pw = null;
    InvDatasetScan ds = null;
    boolean found = false;
    try {
      String dsPath;
      if (pathInfo.indexOf( "/dataset") > 0) {
        dsPath = pathInfo.substring( 0, pathInfo.indexOf( "/dataset") );
      } else if (pathInfo.indexOf( "/catalog") > 0) {
        dsPath = pathInfo.substring( 0, pathInfo.indexOf( "/catalog") );
      } else {
        log.error("RadarServer.datasetInfoXml", "Invalid url request" );
        throw new RadarServerException( "Invalid url request" );
        //return null;
      }

      Iterator parents = cat.getDatasets().iterator();
      InvDatasetImpl top = (InvDatasetImpl) parents.next();
      Iterator tDatasets = top.getDatasets().iterator();
      while (tDatasets.hasNext()) {
        InvDatasetImpl idsi = (InvDatasetImpl) tDatasets.next();
        if (idsi instanceof InvDatasetScan) {
          ds = (InvDatasetScan) idsi;
          if (ds.getPath() == null)
            continue;
          if ( ds.getPath().contains( dsPath )) {
            found = true;
            break;
          } else {
            continue;
          }
        }
      }
    } catch (Throwable e) {
      log.error("RadarServer.datasetInfoXml", e);
      throw new RadarServerException( "CatalogRadarServerController.datasetInfoXml" );
    }
    if ( ! found ) {
      log.error("RadarServer.datasetInfoXml", "Invalid url request" );
      throw new RadarServerException( "Invalid url request" );
      //return null;
    }

    // create dataset
    //pw = res.getWriter();
    //res.setContentType("text/xml; charset=iso-8859-1"); //default

    //pw.println("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Data\" version=\"1.0.1\">\n");
    // add service
    //pw.println("<service name=\"radarServer\" base=\"/thredds/radarServer/\" serviceType=\"DQC\" />\n");

    //pw.println("<dataset ID=\"" + ds.getID() + "\" serviceName=\"radarServer\">");
    model.put( "ID", ds.getID());
    //pw.println("<urlpath>" + ds.getPath() + "</urlpath>");
    model.put( "urlPath", ds.getPath() );
    //pw.println("<dataType>" + ds.getDataType() + "</dataType>");

    //pw.println("<dataFormat>" + ds.getDataFormatType() + "</dataFormat>");
    model.put( "dataFormat", ds.getDataFormatType());
    //pw.println("<serviceName>radarServer</serviceName>");

    //pw.println("<metadata inherited=\"true\">");

    //pw.println("<documentation type=\"summary\">" + ds.getSummary() +
    //    "</documentation>");
    model.put( "documentation", ds.getSummary());
    DateRange dr = ds.getTimeCoverage();
    //pw.println("<TimeSpan>");
    //pw.print("<start>");
    /*
    if (pathInfo.contains("IDD")) {
      pw.print(rm.getStartDateTime(ds.getPath()));
    } else {
      pw.print(dr.getStart().toDateTimeStringISO());
    }
    */    //TODO: check
    //pw.print(dr.getStart().toDateTimeStringISO());
    model.put( "tstart", dr.getStart().toDateTimeStringISO());
    //pw.println("</start>");
    //pw.println("<end>" + dr.getEnd().toDateTimeStringISO() + "</end>");
    model.put( "tend", dr.getEnd().toDateTimeStringISO());
    //pw.println("</TimeSpan>");
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    LatLonRect bb = new LatLonRect();
    gc.setBoundingBox(bb);
    //pw.println("<LatLonBox>");
    //pw.println("<north>" + gc.getLatNorth() + "</north>");
    model.put( "north", gc.getLatNorth());
    //pw.println("<south>" + gc.getLatSouth() + "</south>");
    model.put( "south", gc.getLatSouth());
    //pw.println("<east>" + gc.getLonEast() + "</east>");
    model.put( "east", gc.getLonEast());
    //pw.println("<west>" + gc.getLonWest() + "</west>");
    model.put( "west", gc.getLonWest());
    //pw.println("</LatLonBox>");

    ThreddsMetadata.Variables cvs = (ThreddsMetadata.Variables) ds.getVariables().get(0);
    List vl = cvs.getVariableList();

    //pw.println("<Variables>");
    ArrayList<RsVar> variables = new ArrayList<RsVar>();
    for (int j = 0; j < vl.size(); j++) {
      ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) vl.get(j);
      //pw.println("<variable name=\"" + v.getName() + "\" vocabulary_name=\"" +
      //    v.getVocabularyName() + "\" units=\"" + v.getUnits() + "\" />");
      RsVar rsv = new RsVar();
      rsv.setName( v.getName() );
      rsv.setVname( v.getVocabularyName() );
      rsv.setUnits( v.getUnits() );
      variables.add( rsv );
    }
    model.put( "variables", variables );
    //pw.println("</Variables>");
    //String[] stations = rm.stationsDS(radarType, dataLocation.get(ds.getPath()));
    //rm.printStations(stations, pw, radarType );

    //pw.println("</metadata>");
    //pw.println("</dataset>");
    //pw.println("</catalog>");
    //pw.flush();

    return model;
  }

  /*
   * Used to store the information about a Radar Server variable
   */
  public class RsVar {

    private String name;

    private String vname;

    private String units;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getVname() {
      return vname;
    }

    public void setVname(String vname) {
      this.vname = vname;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }
  }
}
