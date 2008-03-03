/* class RadarServer
 *
 * Library for the Radar Server
 *
 * Finds the near realtime product files for a particular station and time.
 * Outputs either html, dqc  or catalog files.
 *
 * By:  Robb Kambic  09/25/2007
 *
 */

package thredds.server.radarServer;

import thredds.servlet.*;
import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import ucar.nc2.units.DateRange;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Content;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import ucar.unidata.util.StringUtil;
import ucar.unidata.geoloc.LatLonRect;
import ucar.nc2.units.DateFormatter;

public class RadarServer extends AbstractServlet {

    static public final String catName = "radarCollections.xml";
    static public InvCatalogImpl cat = null;
    static public URI catURI;
    static public List datasets;
    static public HashMap<String,String> dataLocation = new HashMap();

    private boolean allow = false;
    private RadarNexradServer rns = null;
    private boolean debug = false;

    protected long getLastModified(HttpServletRequest req) {
        contentPath = ServletUtil.getContentPath(this);
        File file = new File( contentPath + getPath() +catName );
        return file.lastModified();
    }

   // must end with "/"
   protected String getPath() {
     return "servers/";
   }

  protected void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("NetcdfSubsetServer");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showRadar datasets", "Show Radar dataset") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("Radar  Datasets\n");
        for( int j = 0; j < datasets.size(); j++ ) {
            InvDatasetScan ds = (InvDatasetScan) datasets.get( j );
            e.pw.println( ds.getFullName() +" " + ds.getPath() +" "+ ds.getScanLocation());
        }
      }
    };
    debugHandler.addAction(act);
  }

  public void init() throws ServletException  {
      super.init();
      //allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", true);
      //String radarLevel2Dir = ThreddsConfig.get("NetcdfSubsetService.radarLevel2DataDir", "/data/ldm/pub/native/radar/level2/");
      //if (!allow) return;
      contentPath = ServletUtil.getContentPath(this);
      rns = new RadarNexradServer( contentPath );

      // read in radarCollections.xml catalog, creating nexrad/level2 catalog too
      if( cat != null ) return;
      initCat(); //  InvCatalogImpl cat creation
      // get path and location from cat
      List parents = cat.getDatasets();
      for( int i = 0; i < parents.size(); i++ ) {
          InvDataset top = (InvDataset) parents.get( i );
          //String id = top.getID();
          datasets = top.getDatasets(); // dataset scans

          for( int j = 0; j < datasets.size(); j++ ) {
              InvDatasetScan ds = (InvDatasetScan) datasets.get( j );
              if( ds.getPath() != null )
                  dataLocation.put( ds.getPath(), ds.getScanLocation() );
              ds.setXlinkHref( ds.getPath() + "/dataset.xml");
          }
      }
  }

   private void initCat() {
       InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( false ); // no validation
       cat = readCatalog( factory, getPath() +catName, contentPath + getPath() +catName );

   } // end initCat

// get pathInfo and parmameters from servlet call
public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

    PrintWriter pw = null;

    try {
        ServletUtil.logServerAccessSetup( req );
        contentPath = ServletUtil.getContentPath(this);
        if (debug) System.out.println("<documentation>\n"+ req.getQueryString() +"</documentation>\n");
        if( rns == null )  init();
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";
        // default
        if( ! pathInfo.endsWith( "html")) {
            pw = res.getWriter();
            rns.setPW( pw );
            res.setContentType("text/xml; charset=iso-8859-1"); //default
        }

        // return catalog of all datasets
        if(pathInfo.startsWith("/catalog.xml") || pathInfo.startsWith("/dataset.xml") ) {
            if( cat == null) init();
            InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( false ); // no validation
            String catAsString = factory.writeXML( cat );
            pw.println(catAsString);
            res.setStatus( HttpServletResponse.SC_OK );
            return;
        } else if(pathInfo.startsWith("/catalog.html") || pathInfo.startsWith("/dataset.html") ) {
            try {
                HtmlWriter.getInstance().writeCatalog( res, cat, true ); // show catalog as HTML
            } catch (Exception e ) {
                pw = res.getWriter();
                pw.println( "<documentation>\n" );
                pw.println( "CatalogServicesServlet.handleCatalogServiceRequest: "+ pathInfo +"\n" );
                pw.println( "</documentation>\n" );
            }
            return;
        }

        // nexrad level2 and level3 choices
        if( pathInfo.startsWith("/nexrad" )) {


        // nexrad level2 and level3 catalog/dataset
        if( pathInfo.contains("level2/catalog.") || pathInfo.contains("level3/catalog.")
                || pathInfo.contains("level2/dataset.") || pathInfo.contains("level3/dataset.") ) {
            boolean all2 = pathInfo.startsWith( "/nexrad/level2");
            boolean all3 = pathInfo.startsWith( "/nexrad/level3");

            ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
            InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( false );
            factory.writeXML( cat , os, true );
            InvCatalogImpl tCat = factory.readXML( new ByteArrayInputStream( os.toByteArray()), catURI );

            Iterator parents = tCat.getDatasets().iterator();
            while ( parents.hasNext() ) {
                ArrayList<InvDatasetImpl> delete = new ArrayList();
                InvDatasetImpl top = (InvDatasetImpl) parents.next();
                Iterator tDatasets = top.getDatasets().iterator();
                while ( tDatasets.hasNext()) {
                    InvDatasetImpl ds = (InvDatasetImpl) tDatasets.next();
                    if (ds instanceof InvDatasetScan) {
                        InvDatasetScan ids = (InvDatasetScan)ds;
                        if( ids.getPath() == null  )
                            continue;
                        if( all2 && ids.getPath().contains( "level2")) {
                             ids.setXlinkHref( "/thredds/radarServer/"+ ids.getPath() + "/dataset.xml");
                        } else if( all3 && ids.getPath().contains( "level3")) {
                             ids.setXlinkHref( "/thredds/radarServer/"+ids.getPath() + "/dataset.xml");
                        } else {
                             delete.add( ds );
                        }
                    }
                }
                // remove datasets
                for( InvDatasetImpl idi : delete ) {
                    //tCat.removeDatasetByID( idi );
                    top.removeDataset( idi );
                }
            }
            if( pathInfo.endsWith("xml") ) {
                String catAsString = factory.writeXML( tCat );
                pw.println(catAsString);
                res.setStatus( HttpServletResponse.SC_OK );
            } else {
                try {
                    HtmlWriter.getInstance().writeCatalog( res, tCat, true ); // show catalog as HTML
                } catch (Exception e ) {
                    pw = res.getWriter();
                    pw.println( "<documentation>\n" );
                    pw.println( "CatalogServicesServlet.handleCatalogServiceRequest: "+ pathInfo +"\n" );
                    pw.println( "</documentation>\n" );
                }
            }
            return;
        }

        // return stations of dataset
        if( pathInfo.endsWith("stations.xml") ) {
            pathInfo = pathInfo.replace( "/stations.xml", "");
            Element rootElem = new Element( "stationsList" );
            Document doc = new Document(rootElem);
            doc = rns.stationsXML( doc, rootElem, pathInfo.substring( 1 ) );
            XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat() );
            pw.println( fmt.outputString(doc) );
            return;
        }
        // return Dataset information
        if( pathInfo.endsWith("dataset.xml") ) { //|| pathInfo.endsWith("catalog.xml")) {

            pw.println( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Data\" version=\"1.0.1\">\n" );
            // add service
            pw.println( "  <service name=\"radarServer\" base=\"/thredds/radarServer/\" type=\"DQC\" />\n" );
            boolean all2 = pathInfo.equalsIgnoreCase( "/nexrad/level2/dataset.xml");
            boolean all3 = pathInfo.equalsIgnoreCase( "/nexrad/level3/dataset.xml");
            pathInfo = pathInfo.replace( "/dataset.xml", "");
            if( pathInfo.startsWith( "/"))
                pathInfo = pathInfo.substring( 1 );
            for( int i = 0; i < datasets.size(); i++ ) {
                InvDatasetScan ds = (InvDatasetScan) datasets.get( i );
                if( all2 || all3 ) { // no particular ds, all level2 or level3
                    if( (all2 && ds.getPath().contains( "level2"))) {
                        //ok
                    } else if(  (all3 && ds.getPath().contains( "level3"))) {
                        // ok
                    } else {
                        continue;
                    }
                } else if( !(pathInfo.equals( ds.getPath() ) )) {
                    continue;
                }

                pw.println("  <dataset ID=\""+ ds.getID() +"\" datatype=\"radialCollection\" urlpath=\""+
                  ds.getPath() +"\" serviceName=\"radarServer\">");

                pw.println("    <dataType>"+ ds.getDataType() +"</dataType>");

                pw.println("    <dataFormat>"+ ds.getDataFormatType() +"</dataFormat>");

                //pw.println( "   <serviceName>"+ ds.getLocalMetadataInheritable().getServiceName() +"</serviceName>" );
                pw.println( "   <serviceName>radarServer</serviceName>" );

                pw.println("    <metadata inherited=\"true\">");

                pw.println( "    <documentation type=\"summary\">" + ds.getSummary() +
                                "</documentation>" );
                DateRange dr = ds.getTimeCoverage();
                pw.println("      <TimeSpan>");
                pw.println("        <start>"+ dr.getStart().toDateTimeStringISO() +"</start>");
                pw.println("        <end>"+ dr.getEnd().toDateTimeStringISO() +"</end>");
                pw.println("      </TimeSpan>");
                ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
                LatLonRect bb = new LatLonRect();
                gc.setBoundingBox( bb );
                pw.println("      <LatLonBox>");
                pw.println("        <north>"+ gc.getLatNorth() +"</north>");
                pw.println("        <south>"+ gc.getLatSouth() +"</south>");
                pw.println("        <east>"+ gc.getLonEast() +"</east>");
                pw.println("        <west>"+ gc.getLonWest() +"</west>");
                pw.println("      </LatLonBox>");
                //gc.toXML( pw );

                ThreddsMetadata.Variables cvs = (ThreddsMetadata.Variables) ds.getVariables().get(0);
                List vl = cvs.getVariableList();

                //ThreddsMetadata tmi = ds.getLocalMetadataInheritable().getVariables();
                //List variables = ds.getVariables();
                pw.println("      <Variables>");
                for( int j = 0; j < vl.size(); j++ ) {
                    ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) vl.get( j );
                    //<variable name="SpectrumWidth" vocabulary_name="EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Doppler Spectrum Width" units="m/s" />
                    pw.println("        <variable name=\""+ v.getName() +"\" vocabulary_name=\""+
                        v.getVocabularyName() +"\" units=\""+ v.getUnits() +"\" />");
                }
                pw.println("      </Variables>");

                String[] stations = rns.stationsDS( dataLocation.get( ds.getPath() ));
                rns.printStations( stations );
                pw.println( "    </metadata>" );
                pw.println( "  </dataset>" );
            }
            pw.println( "</catalog>" );

            return;
        }

        // return Dataset information in html format
        if( pathInfo.endsWith("dataset.html") ) { //|| pathInfo.endsWith("catalog.html")) {
            pathInfo = pathInfo.replace( "/dataset.html", "");
            Element root = new Element("RadarNexrad");
            Document doc = new Document(root);

            boolean all2 = pathInfo.equalsIgnoreCase( "/nexrad/level2");
            boolean all3 = pathInfo.equalsIgnoreCase( "/nexrad/level3");
            if( pathInfo.startsWith( "/"))
                pathInfo = pathInfo.substring( 1 );
            for( int i = 0; i < datasets.size(); i++ ) {
                InvDatasetScan ds = (InvDatasetScan) datasets.get( i );
                if( all2 || all3 ) { // no particular ds, all level2 or level3
                    if( (all2 && ds.getPath().contains( "level2"))) {
                        //ok
                    } else if(  (all3 && ds.getPath().contains( "level3"))) {
                        // ok
                    } else {
                        continue;
                    }
                } else if( !(pathInfo.equals( ds.getPath() ) )) {
                    continue;
                }

                // at this point a valid dataset
                // fix the location
                root.setAttribute("location", "/thredds/radarServer/"+ ds.getPath() );

                // spatial range
                ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
                LatLonRect bb = new LatLonRect();
                gc.setBoundingBox( bb );
                /*
                pw.println("      <LatLonBox>");
                pw.println("        <north>"+ gc.getLatNorth() +"</north>");
                pw.println("        <south>"+ gc.getLatSouth() +"</south>");
                pw.println("        <east>"+ gc.getLonEast() +"</east>");
                pw.println("        <west>"+ gc.getLonWest() +"</west>");
                pw.println("      </LatLonBox>");
                //gc.toXML( pw );
                */
                String north = Double.toString( gc.getLatNorth() );
                String south = Double.toString( gc.getLatSouth() );
                String east = Double.toString( gc.getLonEast() );
                String west = Double.toString( gc.getLonWest() );

                Element LatLonBox = new Element("LatLonBox");
                LatLonBox.addContent( new Element("north").addContent( north ));
                LatLonBox.addContent( new Element("south").addContent( south));
                LatLonBox.addContent( new Element("east").addContent( east ));
                LatLonBox.addContent( new Element("west").addContent( west ));
                root.addContent( LatLonBox );

                // get the time range
                Element timeSpan = new Element("TimeSpan");
                DateRange dr = ds.getTimeCoverage();
                /*
                pw.println("      <TimeSpan>");
                pw.println("        <start>"+ dr.getStart().toDateTimeStringISO() +"</start>");
                pw.println("        <end>"+ dr.getEnd().toDateTimeStringISO() +"</end>");
                pw.println("      </TimeSpan>");
                */
                timeSpan.addContent(new Element("begin").addContent( dr.getStart().toDateTimeStringISO() ));
                timeSpan.addContent(new Element("end").addContent( dr.getEnd().toDateTimeStringISO() ));
                root.addContent( timeSpan );

                ThreddsMetadata.Variables cvs = (ThreddsMetadata.Variables) ds.getVariables().get(0);
                List vl = cvs.getVariableList();

                //pw.println("      <Variables>");
                for( int j = 0; j < vl.size(); j++ ) {
                    ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) vl.get( j );
                    Element variable = new Element("variable");
                    //<variable name="SpectrumWidth" vocabulary_name="EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Doppler Spectrum Width" units="m/s" />
                    //pw.println("        <variable name=\""+ v.getName() +"\" vocabulary_name=\""+
                    //    v.getVocabularyName() +"\" units=\""+ v.getUnits() +"\" />");
                    variable.setAttribute("name", v.getName() );
                    root.addContent( variable );
                }
                //pw.println("      </Variables>");

                // add pointer to the station list XML
                /*
                Element stnList = new Element("stationList");
                stnList.setAttribute("title", "Available Stations", XMLEntityResolver.xlinkNS);
                stnList.setAttribute("href", "/thredds/radarServer/"+ pathInfo +"/stations.xml",
                        XMLEntityResolver.xlinkNS);
                root.addContent(stnList);
                */
                //String[] stations = rns.stationsDS( dataLocation.get( ds.getPath() ));
                //rns.printStations( stations );

                // add accept list
                Element a = new Element("AcceptList");
                //elem.addContent(new Element("accept").addContent("raw"));
                a.addContent(new Element("accept").addContent("xml"));
                root.addContent( a );
            }
            ServerMethods sm = new  ServerMethods();
            InputStream xslt = sm.getInputStream(contentPath + getPath() + "radar.xsl", RadarServer.class);

            try {
                XSLTransformer transformer = new XSLTransformer(xslt);
                Document html = transformer.transform(doc);
                XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
                String infoString = fmt.outputString(html);
                res.setContentType("text/html; charset=iso-8859-1");
                pw = res.getWriter();
                pw.println( infoString );

            } catch (Exception e) {
                ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "RadarServer dataset.html error");
            }
            return;
        }

        // radar Nexrad query
        if( req.getQueryString() != null) {
            rns.radarNexradQuery( req, res );
        } else {
            pw.println( "<documentation>\n" );
            pw.println( "No Query given: "+ pathInfo +"\n" );
            pw.println( "</documentation>\n" );
        }

        } else {
            pw.println( "<documentation>\n" );
            pw.println( "Request not implemented: "+ pathInfo +"\n" );
            pw.println( "</documentation>\n" );
        }
    } catch (Throwable t) {
       ServletUtil.handleException(t, res);
    }
    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);
} // end doGet

    /**
     * Does the actual work of reading a catalog.
     *
     * @param factory  use this InvCatalogFactory
     * @param path reletive path starting from content root
     * @param catalogFullPath absolute location on disk
     * @return the InvCatalogImpl, or null if failure
     */
    private InvCatalogImpl readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {

      InvCatalogImpl acat;
      try {
        catURI = new URI("file:" + StringUtil.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
      }
      catch (URISyntaxException e) {
        log.error("readCatalog(): URISyntaxException=" + e.getMessage());
        return null;
      }

      // read the catalog
      log.debug("readCatalog(): full path=" + catalogFullPath + "; path=" + path);
      FileInputStream ios = null;
      try {
        ios = new FileInputStream(catalogFullPath);
        acat = factory.readXML(ios, catURI );
      } catch (Throwable t) {
        log.error("readCatalog(): Exception on catalog=" + catalogFullPath + " " + t.getMessage()+"\n log="+cat.getLog(), t);
        return null;
      }
      finally {
        if (ios != null) {
          try {
            ios.close();
          }
          catch (IOException e) {
            log.error("readCatalog(): error closing" + catalogFullPath);
          }
        }
      }
      return acat;
    }
} // end RadarServer
