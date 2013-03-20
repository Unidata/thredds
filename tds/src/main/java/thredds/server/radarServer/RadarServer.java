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
/* class RadarServer
 *
 * Library for the TDS Radar Server
 *
 * Currently servers Level2 and Level3 Radar files.
 * Finds the files for a particular station, product and time.
 * Outputs either html or xml files.
 *
 * By:  Robb Kambic  09/25/2007
 *
 */

package thredds.server.radarServer;

import thredds.servlet.*;
import thredds.catalog.*;
import ucar.nc2.time.CalendarDateRange;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.XSLTransformer;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.StringUtil2;

public class RadarServer extends AbstractServlet {
  public enum RadarType {
    nexrad, terminal
  }

  static public InvCatalogImpl cat;
  static public final String catName = "radarCollections.xml";
  static public URI catURI;
  static public List datasets;
  static public HashMap<String, String> dataLocation = new HashMap<String, String>();
  static public RadarMethods rm;
  private boolean allow = false;
  private boolean debug = false;

  protected long getLastModified(HttpServletRequest req) {
    contentPath = ServletUtil.getContentPath();
    File file = new File(contentPath + getPath() + catName);
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
        for (int j = 0; j < datasets.size(); j++) {
          InvDatasetScan ds = (InvDatasetScan) datasets.get(j);
          e.pw.println(ds.getFullName() + " " + ds.getPath() + " " + ds.getScanLocation());
        }
      }
    };
    debugHandler.addAction(act);
  }

  public void init() throws ServletException {
    super.init();
    //allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", true);
    //String radarLevel2Dir = ThreddsConfig.get("NetcdfSubsetService.radarLevel2DataDir", "/data/ldm/pub/native/radar/level2/");
    //if (!allow) return;
    contentPath = ServletUtil.getContentPath();
    rm = new RadarMethods(contentPath, logServerStartup);

    // read in radarCollections.xml catalog
    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false); // no validation
    cat = readCatalog(factory, getPath() + catName, contentPath + getPath() + catName);
    if (cat == null) {
      logServerStartup.info("cat initialization failed");
      return;
    }
    //URI tmpURI = cat.getBaseURI();
    cat.setBaseURI(catURI);
    // get path and location from cat
    List parents = cat.getDatasets();
    for (int i = 0; i < parents.size(); i++) {
      InvDataset top = (InvDataset) parents.get(i);
      datasets = top.getDatasets(); // dataset scans

      for (int j = 0; j < datasets.size(); j++) {
        InvDatasetScan ds = (InvDatasetScan) datasets.get(j);
        if (ds.getPath() != null) {
          dataLocation.put(ds.getPath(), ds.getScanLocation());
          logServerStartup.info("path =" + ds.getPath() + " location =" + ds.getScanLocation());
        }
        ds.setXlinkHref(ds.getPath() + "/dataset.xml");
      }
    }
    logServerStartup.info(getClass().getName() + " initialization complete ");
  } // end init

  // get pathInfo and parmameters from servlet call
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    PrintWriter pw = null;
    try {
      long startms = System.currentTimeMillis();

      if (cat == null || rm.nexradList == null) { // something major wrong
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "radarServer Radar Station/Catalog initialization problem");
        return;
      }
      // setup
      String pathInfo = req.getPathInfo();
      if (pathInfo == null) pathInfo = "";
      RadarType radarType = RadarType.nexrad;  //default
      if (pathInfo.indexOf('/', 1) > 1) {
        String rt = pathInfo.substring(1, pathInfo.indexOf('/', 1));
        radarType = RadarType.valueOf(rt);
      }
      // default is xml, assume errors will be recorded by logger from this point
      if (!pathInfo.endsWith("html")) {
        pw = res.getWriter();
        res.setContentType("text/xml; charset=iso-8859-1"); //default
      }
      // radar  query
      if (req.getQueryString() != null) {
        //log.debug("RadarServer query ="+ req.getQueryString() );
        if (log.isDebugEnabled()) log.debug("<documentation>\n" + req.getQueryString() + "</documentation>\n");
        rm.radarQuery(radarType, req, res, pw);
        if (log.isDebugEnabled()) log.debug("after doGet " + (System.currentTimeMillis() - startms));
        pw.flush();
        return;
      }
      // return radarCollections catalog   xml or html
      if (pathInfo.startsWith("/catalog.xml") || pathInfo.startsWith("/dataset.xml")) {
        InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false); // no validation
        String catAsString = factory.writeXML(cat);
        pw.println(catAsString);
        res.setStatus(HttpServletResponse.SC_OK);
        pw.flush();
        return;
      } else if (pathInfo.startsWith("/catalog.html") || pathInfo.startsWith("/dataset.html")) {
        try {
          int i = HtmlWriter.getInstance().writeCatalog(req, res, cat, true); // show catalog as HTML
        } catch (Exception e) {
          log.error("Radar HtmlWriter failed ", e);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "radarServer HtmlWriter error " + pathInfo);
          return;
        }
        return;
      }
      // level2 and level3 catalog/dataset
      if (pathInfo.contains("level2/catalog.") || pathInfo.contains("level3/catalog.")
          || pathInfo.contains("level2/dataset.") || pathInfo.contains("level3/dataset.")) {
        level2level3catalog(radarType, pathInfo, pw, req, res);
        return;
      }
      // return stations of dataset
      if (pathInfo.endsWith("stations.xml")) {
        pathInfo = pathInfo.replace("/stations.xml", "");
        Element rootElem = new Element("stationsList");
        Document doc = new Document(rootElem);
        doc = rm.stationsXML(radarType, doc, rootElem, pathInfo.substring(1));
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        pw.println(fmt.outputString(doc));
        pw.flush();
        return;
      }
      // return specific dataset information, ie IDD
      if (pathInfo.endsWith("dataset.xml") || pathInfo.endsWith("catalog.xml")) {
        datasetInfoXml(radarType, pathInfo, pw);
        return;
      }
      // needs work nobody using it now
      // return Dataset information in html form format
      if (pathInfo.endsWith("dataset.html") || pathInfo.endsWith("catalog.html")) {
        datasetInfoHtml(radarType, pathInfo, pw, res);
        return;
      }
      // mal formed request with no exceptions
      res.sendError(HttpServletResponse.SC_NOT_FOUND);

    } catch (FileNotFoundException e) {
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_NOT_FOUND);

    } catch (Throwable e) {
      log.error("RadarServer.doGet failed", e);
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  } // end doGet

  private void level2level3catalog(RadarType radarType, String pathInfo, PrintWriter pw, HttpServletRequest req, HttpServletResponse res)
      throws IOException {

    try {
      String type;
      if (pathInfo.contains("level2"))
        type = radarType.toString() + "/level2";
      else
        type = radarType.toString() + "/level3";

      ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
      InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false);
      factory.writeXML(cat, os, true);
      InvCatalogImpl tCat = factory.readXML(new ByteArrayInputStream(os.toByteArray()), catURI);

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
      if (pathInfo.endsWith("xml")) {
        String catAsString = factory.writeXML(tCat);
        pw.println(catAsString);
        pw.flush();
      } else {
        HtmlWriter.getInstance().writeCatalog(req, res, tCat, true); // show catalog as HTML
      }
    } catch (Throwable e) {
      log.error("RadarServer.level2level3catalog failed", e);
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    return;
  }

  private void datasetInfoXml(RadarType radarType, String pathInfo, PrintWriter pw) throws IOException {

    try {
      pw.println("<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Data\" version=\"1.0.1\">\n");
      // add service
      pw.println("  <service name=\"radarServer\" base=\"/thredds/radarServer/\" serviceType=\"DQC\" />\n");
      pathInfo = pathInfo.replace("/dataset.xml", "");
      pathInfo = pathInfo.replace("/catalog.xml", "");
      if (pathInfo.startsWith("/"))
        pathInfo = pathInfo.substring(1);
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetScan ds = (InvDatasetScan) datasets.get(i);
        if (!(pathInfo.equals(ds.getPath()))) {
          continue;
        }

        pw.println("  <dataset ID=\"" + ds.getID() + "\" serviceName=\"radarServer\">");

        pw.println("    <urlpath>" + ds.getPath() + "</urlpath>");
        pw.println("    <dataType>" + ds.getDataType() + "</dataType>");

        pw.println("    <dataFormat>" + ds.getDataFormatType() + "</dataFormat>");

        pw.println("   <serviceName>radarServer</serviceName>");

        pw.println("    <metadata inherited=\"true\">");

        pw.println("    <documentation type=\"summary\">" + ds.getSummary() +
            "</documentation>");
        CalendarDateRange dr = ds.getCalendarDateCoverage();
        pw.println("      <TimeSpan>");
        pw.print("        <start>");
        if (pathInfo.contains("IDD")) {
          pw.print(rm.getStartDateTime(ds.getPath()));
        } else {
          pw.print(dr.getStart().toString());
        }
        pw.println("</start>");
        pw.println("        <end>" + dr.getEnd().toString() + "</end>");
        pw.println("      </TimeSpan>");
        ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
        LatLonRect bb = new LatLonRect();
        gc.setBoundingBox(bb);
        pw.println("      <LatLonBox>");
        pw.println("        <north>" + gc.getLatNorth() + "</north>");
        pw.println("        <south>" + gc.getLatSouth() + "</south>");
        pw.println("        <east>" + gc.getLonEast() + "</east>");
        pw.println("        <west>" + gc.getLonWest() + "</west>");
        pw.println("      </LatLonBox>");

        ThreddsMetadata.Variables cvs = (ThreddsMetadata.Variables) ds.getVariables().get(0);
        List vl = cvs.getVariableList();

        pw.println("      <Variables>");
        for (int j = 0; j < vl.size(); j++) {
          ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) vl.get(j);
          pw.println("        <variable name=\"" + v.getName() + "\" vocabulary_name=\"" +
              v.getVocabularyName() + "\" units=\"" + v.getUnits() + "\" />");
        }
        pw.println("      </Variables>");
        String[] stations = rm.stationsDS(radarType, dataLocation.get(ds.getPath()));
        rm.printStations(stations, pw, radarType );

        pw.println("    </metadata>");
        pw.println("  </dataset>");
      }
      pw.println("</catalog>");
      pw.flush();
    } catch (Throwable e) {
      log.error("RadarServer.datasetInfoXml", e);
    }
    return;
  }

  private void datasetInfoHtml(RadarType radarType, String pathInfo, PrintWriter pw, HttpServletResponse res)
      throws IOException {
    pathInfo = pathInfo.replace("/dataset.html", "");
    pathInfo = pathInfo.replace("/catalog.html", "");
    Element root = new Element("RadarNexrad");
    Document doc = new Document(root);

    if (pathInfo.startsWith("/"))
      pathInfo = pathInfo.substring(1);
    for (int i = 0; i < datasets.size(); i++) {
      InvDatasetScan ds = (InvDatasetScan) datasets.get(i);
      if (!(pathInfo.equals(ds.getPath()))) {
        continue;
      }

      // at this point a valid dataset
      // fix the location
      root.setAttribute("location", "/thredds/radarServer/" + ds.getPath());

      // spatial range
      ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
      LatLonRect bb = new LatLonRect();
      gc.setBoundingBox(bb);
      String north = Double.toString(gc.getLatNorth());
      String south = Double.toString(gc.getLatSouth());
      String east = Double.toString(gc.getLonEast());
      String west = Double.toString(gc.getLonWest());

      Element LatLonBox = new Element("LatLonBox");
      LatLonBox.addContent(new Element("north").addContent(north));
      LatLonBox.addContent(new Element("south").addContent(south));
      LatLonBox.addContent(new Element("east").addContent(east));
      LatLonBox.addContent(new Element("west").addContent(west));
      root.addContent(LatLonBox);

      // get the time range
      Element timeSpan = new Element("TimeSpan");
      CalendarDateRange dr = ds.getCalendarDateCoverage();
      timeSpan.addContent(new Element("begin").addContent(dr.getStart().toString()));
      timeSpan.addContent(new Element("end").addContent(dr.getEnd().toString()));
      root.addContent(timeSpan);

      ThreddsMetadata.Variables cvs = (ThreddsMetadata.Variables) ds.getVariables().get(0);
      List vl = cvs.getVariableList();

      for (int j = 0; j < vl.size(); j++) {
        ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) vl.get(j);
        Element variable = new Element("variable");
        variable.setAttribute("name", v.getName());
        root.addContent(variable);
      }

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
      a.addContent(new Element("accept").addContent("xml"));
      a.addContent(new Element("accept").addContent("html"));
      root.addContent(a);
    }
    ServerMethods sm = new ServerMethods(log);
    InputStream xslt = sm.getInputStream(contentPath + getPath() + "radar.xsl", RadarServer.class);

    try {
      // what's wrong here xslt = getXSLT( "radar.xsl" );
      XSLTransformer transformer = new XSLTransformer(xslt);
      Document html = transformer.transform(doc);
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      String infoString = fmt.outputString(html);
      res.setContentType("text/html; charset=iso-8859-1");
      pw = res.getWriter();
      pw.println(infoString);
      pw.flush();

    } catch (Exception e) {
      log.error("radarServer reading " + contentPath + getPath() + "radar.xsl");
      log.error("radarServer XSLTransformer problem for web form ", e);
    }
    finally {
      if (xslt != null) {
        try {
          xslt.close();
        }
        catch (IOException e) {
          log.error("radarServer radar.xsl: error closing" +
              contentPath + getPath() + "radar.xsl");
        }
      }
    }
    return;
  }

  /**
   * Does the actual work of reading a catalog.
   *
   * @param factory         use this InvCatalogFactory
   * @param path            reletive path starting from content root
   * @param catalogFullPath absolute location on disk
   * @return the InvCatalogImpl, or null if failure
   */
  private InvCatalogImpl readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {

    InvCatalogImpl acat;
    try {
      catURI = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    }
    catch (URISyntaxException e) {
      logServerStartup.info("radarServer readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    // read the catalog
    logServerStartup.info("radarServer readCatalog(): full path=" + catalogFullPath + "; path=" + path);
    FileInputStream ios = null;
    try {
      ios = new FileInputStream(catalogFullPath);
      acat = factory.readXML(ios, catURI);
    } catch (Throwable t) {
      logServerStartup.error("radarServer readCatalog(): Exception on catalog=" +
          catalogFullPath + " " + t.getMessage()); //+"\n log="+cat.getLog(), t);
      return null;
    }
    finally {
      if (ios != null) {
        try {
          ios.close();
        }
        catch (IOException e) {
          logServerStartup.info("radarServer readCatalog(): error closing" + catalogFullPath);
        }
      }
    }
    return acat;
  }

  private InputStream getXSLT(String xslName)  throws IOException {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

  public static void main(String args[]) throws IOException {

    // Function References
    String path = "http://motherlode.ucar.edu:8081/thredds/radarServer/nexrad/level3/IDD/dataset.xml";

    try {
      catURI = new URI(StringUtil2.escape(path, "/:-_."));
    }
    catch (URISyntaxException e) {
      System.out.println("radarServer main: URISyntaxException=" + e.getMessage());
      return;
    }

    // read the catalog
    System.out.println("radarServer main: full path=" + path);
    InputStream ios = null;
    try {

      URL url = new URL(path);
      ios = url.openStream();
      //ios = new FileInputStream(path);
      //acat = factory.readXML(ios, catURI );
      BufferedReader dataIS =
          new BufferedReader(new InputStreamReader(ios));

      while (true) {
        String line = dataIS.readLine();
        if (line == null)
          break;
        System.out.println(line);
      }

    } catch (Throwable t) {
      System.out.println("radarServer main: Exception on catalog=" +
          path + " " + t.getMessage());
      return;
    }
    finally {
      if (ios != null) {
        try {
          ios.close();
        }
        catch (IOException e) {
          System.out.println("radarServer main: error closing" + path);
        }
      }
    }
    return;
  }
} // end RadarServer
