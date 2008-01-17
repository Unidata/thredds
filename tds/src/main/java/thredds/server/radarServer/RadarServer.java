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
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import ucar.unidata.util.StringUtil;
import ucar.unidata.geoloc.LatLonRect;

public class RadarServer extends AbstractServlet {

    static protected StringBuilder catalog = null;
    static protected InvCatalogImpl cat = null;
    static protected List datasets;
    static protected final String catName = "radarCollections.xml";

    //static protected HashMap<String,String> dataPath;
    //static protected HashMap<String,String> dataLocation;

    private boolean allow = false;
    private RadarLevel2Server rl2 = null;
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

    act = new DebugHandler.Action("showRadarLevel2Files", "Show RadarLevel2 Files") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("RadarLevel2 Files\n");
        //ArrayList<RadarLevel2Collection.Dataset> list = rl2c.getDatasets();
        for( int j = 0; j < datasets.size(); j++ ) {
            InvDatasetScan ds = (InvDatasetScan) datasets.get( j );
            e.pw.println(" " + ds);
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
      rl2 = new RadarLevel2Server( contentPath );

      // read in radarCollections.xml catalog, creating nexrad/level2 catalog too
      if( catalog != null || cat != null ) return;
      //initCatalog(); // StringBuilder catalog creation
      initCat(); //  InvCatalogImpl cat creation
      // get path and location from cat
      List parents = cat.getDatasets();
      for( int i = 0; i < parents.size(); i++ ) {
          InvDataset top = (InvDataset) parents.get( i );
          //String id = top.getID();
          datasets = top.getDatasets(); // dataset scans

          for( int j = 0; j < datasets.size(); j++ ) {
              InvDatasetScan ds = (InvDatasetScan) datasets.get( j );
              //id = ds.getID();
              if( ds.getPath() != null )
                  RadarLevel2Server.dataLocation.put( ds.getPath(), ds.getScanLocation() );
          }
      }
  }

  private void initCatalog() {

      catalog = new StringBuilder( 3072 );
      RadarLevel2Server.catalog = new StringBuilder( 3072 );
      RadarLevel2Server.dataPath = new HashMap();
      RadarLevel2Server.dataLocation = new HashMap();

      DocumentBuilder parser;
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);

      try {
          // add catalog element
          catalog.append( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Data\" version=\"1.0.1\">\n" );
          RadarLevel2Server.catalog.append( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Data\" version=\"1.0.1\">\n" );
          // add service
          catalog.append( "  <service name=\"radarServer\" base=\"/thredds/radarServer\" type=\"DQC\" />\n" );
          RadarLevel2Server.catalog.append( "  <service name=\"radarServer\" base=\"/thredds/radarServer\" type=\"DQC\" />\n" );

          parser = factory.newDocumentBuilder();
          InputStream is = new FileInputStream(contentPath + getPath() +catName);
          org.w3c.dom.Document doc = parser.parse(is);
            //System.out.println( "root=" + doc.getDocumentElement().getTagName() );
          NodeList d = doc.getElementsByTagName("datasetScan");
          for (int i = 0; i < d.getLength(); i++) {
              //System.out.println( "node=" + d.item( i ).getNodeName() );
              NamedNodeMap da  = d.item(i).getAttributes();  // datasetScan attributes
              String id = "", path = "", location = "", newDS = "";
              for (int j = 0; j < da.getLength(); j++) {
                    if (da.item(j).getNodeName().equals("ID")) {
                        id = da.item(j).getNodeValue();
                    } else if (da.item(j).getNodeName().equals("path")) {
                        path = da.item(j).getNodeValue();
                    } else if (da.item(j).getNodeName().equals("location")) {
                        location = da.item(j).getNodeValue();
                    }
              }
              newDS = "  <dataset ID=\""+ id +"\" datatype=\"radialCollection\" urlpath=\""+
                  path +"\" serviceName=\"radarServer\">\n";
              if( path.contains( "nexrad/level2")) {
                  RadarLevel2Server.catalog.append( newDS );
                  RadarLevel2Server.catalog.append( "</dataset>\n" );
                  RadarLevel2Server.dataPath.put( id, path);
                  RadarLevel2Server.dataLocation.put( path, location);
             }
             catalog.append( newDS );
             catalog.append( "</dataset>\n" );
             /*
             NodeList c = d.item(i).getChildNodes();  //Children of datasetScan
             for (int j = 0; j < c.getLength(); j++) {
                 if ( c.item(j).getNodeName().equals("documentation")) {
                        continue;  //kludge for #text nodes
                 }
             }
             */
          }
          catalog.append( "</catalog>\n" );
          RadarLevel2Server.catalog.append( "</catalog>\n" );

      } catch (SAXException e) {
          e.printStackTrace();
      } catch (IOException e) {
          e.printStackTrace();
      } catch (ParserConfigurationException e) {
          e.printStackTrace();
      }

  } // end initCatalog

   private void initCat() {
       InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( false ); // no validation
       readCatalog( factory, getPath() +catName, contentPath + getPath() +catName );
   } // end initCat

// get parmameters from servlet call
public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

    PrintWriter pw;

    try {
        ServletUtil.logServerAccessSetup( req );
        if (debug) System.out.println(req.getQueryString());
        pw = res.getWriter();
        if( rl2 == null )  init();
        rl2.setPW( pw );
        res.setContentType("text/xml; charset=iso-8859-1");
        contentPath = ServletUtil.getContentPath(this);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        // return catalog of all datasets
        if(pathInfo.startsWith("/catalog.xml")) {
            if( catalog == null) init();
            pw.println( catalog.toString() );
            return;
        } else if(pathInfo.startsWith("/catalog.html")) {
            res.setContentType("text/html; charset=iso-8859-1");
            if( catalog == null) init();
            // convert to html
            pw.println( catalog.toString() );
            return;
        }
        // nexrad2 catalog
        if(pathInfo.startsWith("/nexrad/level2/catalog.xml")) {
            //if( RadarLevel2Server.catalog == null) init();
            //pw.println( RadarLevel2Server.catalog.toString() );
            InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( false ); // no validation
            String catAsString = factory.writeXML( cat );
            res.setStatus( HttpServletResponse.SC_OK );
            pw.println(catAsString);
            return;
        } else if(pathInfo.startsWith("/nexrad/level2/catalog.html")) {
            res.setContentType("text/html; charset=iso-8859-1");
            if( RadarLevel2Server.catalog == null) init();
            // convert to html needs to be done
            pw.println( RadarLevel2Server.catalog.toString() );
            return;
        }
        // return stations of dataset
        if( pathInfo.startsWith("/nexrad/level2") &&
            pathInfo.endsWith("stations.xml") ) {
            Element rootElem = new Element( "stationsList" );
            Document doc = new Document(rootElem);
            doc = rl2.stationsXML( doc, rootElem, pathInfo.replace( "/stations.xml", ""));
            XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
            pw.println( fmt.outputString(doc) );
            return;
        }
        // return Dataset information
        if( pathInfo.startsWith("/nexrad/level2") &&
            pathInfo.endsWith("dataset.xml") ) {
            pw.println( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Data\" version=\"1.0.1\">\n" );
            // add service
            pw.println( "  <service name=\"radarServer\" base=\"/thredds/radarServer\" type=\"DQC\" />\n" );
            boolean all = pathInfo.equalsIgnoreCase( "/nexrad/level2/dataset.xml");
            String dsPath = pathInfo.replace( "/dataset.xml", "");
            if( dsPath.startsWith( "/"))
                dsPath = dsPath.substring( 1 );
            for( int i = 0; i < datasets.size(); i++ ) {
                InvDatasetScan ds = (InvDatasetScan) datasets.get( i );
                if( !(dsPath.equals( ds.getPath() ) ||  all )) continue;
            //  pw.println(" " + ds);
                pw.println("  <dataset ID=\""+ ds.getID() +"\" datatype=\"radialCollection\" urlpath=\""+
                  ds.getPath() +"\" serviceName=\"radarServer\">");
                pw.println("    <dataType>"+ ds.getDataType() +"</dataType>");
                pw.println("    <dataFormat>"+ ds.getDataFormatType() +"</dataFormat>");
                pw.println( "   <serviceName>"+ ds.getLocalMetadataInheritable().getServiceName() +"</serviceName>" );
                pw.println("    <metadata inherited=\"true\">");

                pw.println( "    <documentation type=\"summary\">" + ds.getSummary() +
                                "</documentation>" );
                /*
                what!!!
                List list = ds.getDocumentation();
                for( int j = 0; j < list.size(); j++ ) {
                    InvDocumentation idoc = (InvDocumentation) list.get( j );
                    if( idoc.getType() != null && idoc.getType().equals( "summary"))
                        pw.println( "    <documentation type=\"summary\">" + idoc.getInlineContent() +
                                "</documentation>" );
                }
                */
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
                
                String[] stations = rl2.stationsDS( ds.getPath() );
                rl2.printStations( stations );
                pw.println( "    </metadata>" );
                pw.println( "  </dataset>" );
            }
            pw.println( "</catalog>" );

            return;
        }

        // return DQC
        if( pathInfo.startsWith("/nexrad/level2") &&
            pathInfo.endsWith("DQC.xml") ) {
            rl2.returnDQC( pathInfo.replace( "/DQC.xml", ""));
            return;
        }

        // radarLevel2 query
        if( pathInfo.startsWith("/nexrad/level2") ) {
            rl2.radarLevel2Query( req, res );
            return;
        }
        pw.println( "<documentation>\n" );
        pw.println( "Request not implemented: "+ pathInfo +"\n" );
        pw.println( "</documentation>\n" );

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
    private void readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {
      URI uri;
      try {
        uri = new URI("file:" + StringUtil.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
      }
      catch (URISyntaxException e) {
        log.error("readCatalog(): URISyntaxException=" + e.getMessage());
        return;
      }

      // read the catalog
      log.debug("readCatalog(): full path=" + catalogFullPath + "; path=" + path);
      //InvCatalogImpl cat = null;
      FileInputStream ios = null;
      try {
        ios = new FileInputStream(catalogFullPath);
        cat = factory.readXML(ios, uri);
      } catch (Throwable t) {
        log.error("readCatalog(): Exception on catalog=" + catalogFullPath + " " + t.getMessage()+"\n log="+cat.getLog(), t);
        return;
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
      //return cat;
    }

} // end RadarServer
