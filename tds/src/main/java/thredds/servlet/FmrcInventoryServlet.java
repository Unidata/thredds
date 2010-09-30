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

package thredds.servlet;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import ucar.nc2.dt.fmrc.FmrcInventory;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.dt.fmrc.FmrcReport;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import thredds.catalog.InvDatasetFmrc;
import ucar.unidata.util.StringUtil;

/**
 * Servlet shows Forecast Model Run Collection Inventory.
 *
 * @author caron
 */
public class FmrcInventoryServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 fmrCache = null;
  private boolean debug = false;
  private static String defPath; // default path where definition files are kept

  static public void setDefinitionDirectory(File defDir) {
    defPath = defDir.getPath() + '/';
  }

  protected String getPath() {
    return "modelInventory/";
  }

  protected void makeDebugActions() {
  }

  public void init() throws ServletException
  {
    super.init();
    logServerStartup.info( getClass().getName() + " initialization done -  " + UsageLog.closingMessageNonRequestContext() );
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    log.info(UsageLog.setupRequestContext(req));

    String path = req.getPathInfo();
    String query = req.getQueryString();

    debug = Debug.isSet("FmrcInventoryServlet");
    if (debug) System.out.println("path=" + path + " query=" + query);

    String varName;
    DataRootHandler h = DataRootHandler.getInstance();
    DataRootHandler.DataRootMatch match = h.findDataRootMatch(req);
    if ((match == null) || (match.dataRoot.fmrc == null)) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
      return;
    }

    InvDatasetFmrc.InventoryParams params = match.dataRoot.fmrc.getFmrcInventoryParams();
    if (params == null) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
      return;
    }

    FmrcInventory fmr;
    try {

      if (debug) System.out.println("  FmrcInventoryParams=" + params + " for path=" + match.rootPath);
      String fmrcDefinitionPath = defPath;
      String collectionName = params.def;
      File file = new File(params.def);
      if (file.isAbsolute()) { // allow absolute path of definition files
        int pos = params.def.lastIndexOf("/");
        if (pos > 0) {
          fmrcDefinitionPath = params.def.substring(0, pos + 1);
          collectionName = params.def.substring(pos + 1);
        }
      }

      String fmrInvOpenType = ThreddsConfig.get("FmrcInventory.openType", "");
      int mode = fmrInvOpenType.equalsIgnoreCase("XML_ONLY") ? ForecastModelRunInventory.OPEN_XML_ONLY : ForecastModelRunInventory.OPEN_NORMAL;
      if (debug)
        System.out.println("  FmrcInventory.make path=" + fmrcDefinitionPath + " name= " + collectionName + " location= " + params.location
            + " suffix= " + params.suffix + " mode= " + mode);

      fmr = FmrcInventory.makeFromDirectory(fmrcDefinitionPath, collectionName, fmrCache, params.location, params.suffix, mode);

    } catch (Exception e) {
      e.printStackTrace();
      log.error("ForecastModelRunCollection.make", e);
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
      return;
    }

    if (fmr == null) {
      log.warn("ForecastModelRunCollection.make");
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
      return;
    }

    String define = req.getParameter("define");
    if (define != null) {
      showDefinition(res, fmr, define);
      return;
    }

    String report = req.getParameter("report");
    if (report != null) {
      try {
        report(fmr, res, report.equals("missing"));
      } catch (Exception e) {
        e.printStackTrace();
        log.error("report", e);
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
        if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      return;
    }

    varName = match.remaining;
    if (varName.startsWith("/"))
      varName = varName.substring(1);
    if (varName.endsWith("/"))
      varName = varName.substring(0, varName.length() - 1);

    String offsetHour = req.getParameter("offsetHour");
    if (offsetHour != null) {
      showOffsetHour(res, fmr, varName, offsetHour);
      return;
    }

    boolean wantXML = req.getParameter("wantXML") != null;

    showInventory(res, fmr, varName, query, wantXML);
  }

  private void showOffsetHour(HttpServletResponse res, FmrcInventory fmrc, String varName, String offsetHour) throws IOException {
    res.setContentType("text/plain; charset=iso-8859-1");
    String contents = fmrc.showOffsetHour(varName, offsetHour);

    OutputStream out = res.getOutputStream();
    out.write(contents.getBytes());
    out.flush();
    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, contents.length()));
  }

  private void report(FmrcInventory fmrc, HttpServletResponse res, boolean showMissing) throws Exception {
    res.setContentType("text/plain; charset=iso-8859-1");
    OutputStream out = res.getOutputStream();
    PrintStream ps = new PrintStream(out);

    FmrcReport report = new FmrcReport();
    report.report(fmrc, ps, showMissing);
    ps.flush();

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
  }

  private void reportAll(HttpServletResponse res, boolean showMissing) throws Exception {
    res.setContentType("text/plain; charset=iso-8859-1");
    OutputStream out = res.getOutputStream();
    PrintStream ps = new PrintStream(out);

    String[] paths = getDatasetPaths();
    for (String path1 : paths) {
      String path = "http://motherlode.ucar.edu:8080" + path1;

      int pos = path.indexOf("model/");
      String path2 = path.substring(pos + 6);
      String name = StringUtil.replace(path2, '/', "-");
      if (name.startsWith("-")) name = name.substring(1);
      if (name.endsWith("-")) name = name.substring(0, name.length() - 1);
      String dir = "/data/ldm/pub/native/grid/" + path2;

      //System.out.println("  fmrcDefinitionPath="+contentPath+" name="+name+" dir="+dir);
      ps.println("\n*******Dataset" + dir);
      FmrcInventory fmrc = FmrcInventory.makeFromDirectory(contentPath, name, fmrCache, dir, ".grib1",
          ForecastModelRunInventory.OPEN_XML_ONLY);
      if (null == fmrc) {
        ps.println("  ERROR - no files were found");
      } else {
        FmrcReport report = new FmrcReport();
        report.report(fmrc, ps, showMissing);
      }
      ps.flush();
    }

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
  }

  private void showDefinition(HttpServletResponse res, FmrcInventory fmrc, String define) throws IOException {
    res.setContentType("text/xml; charset=iso-8859-1");
    FmrcDefinition def = fmrc.getDefinition();

    if (define.equals("write")) {
      FileOutputStream fos = new FileOutputStream(fmrc.getDefinitionPath());
      def = new FmrcDefinition();
      def.makeFromCollectionInventory(fmrc);
      def.writeDefinitionXML(fos);
      if (debug) System.out.println(" write to " + fmrc.getDefinitionPath());

    } else if ((def != null) && (define.equals("addVert"))) {
      FileOutputStream fos = new FileOutputStream(fmrc.getDefinitionPath());
      def.addVertCoordsFromCollectionInventory(fmrc);
      def.writeDefinitionXML(fos);
      if (debug) System.out.println(" write to " + fmrc.getDefinitionPath());
    }

    if (def == null) {
      def = new FmrcDefinition();
      def.makeFromCollectionInventory(fmrc);
    }

    String xmlString = def.writeDefinitionXML();
    OutputStream out = res.getOutputStream();
    out.write(xmlString.getBytes());
    out.flush();
    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, xmlString.length()));
  }

  private void showInventory(HttpServletResponse res, FmrcInventory fmr, String varName, String type, boolean wantXml) throws IOException {
    if (varName.length() == 0)
      varName = null;
    boolean matrix = (type != null) && (type.equalsIgnoreCase("Matrix"));

    String infoString;
    try {
      if (wantXml) {
        infoString = fmr.writeMatrixXML(varName);

      } else {
        Document doc;
        InputStream xslt;
        if (varName == null) {
          xslt = matrix ? getXSLT("fmrMatrix.xsl") : getXSLT("fmrOffset.xsl");
          doc = fmr.makeMatrixDocument();
        } else {
          xslt = matrix ? getXSLT("fmrMatrixVariable.xsl") : getXSLT("fmrOffsetVariable.xsl");
          doc = fmr.makeMatrixDocument(varName);
        }

        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);
      }

    } catch (IllegalArgumentException e) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;

    } catch (Exception e) {
      log.error("ForecastModelRunServlet internal error", e);
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ForecastModelRunServlet internal error");
      return;
    }

    res.setContentLength(infoString.length());
    if (wantXml)
      res.setContentType("text/xml; charset=iso-8859-1");
    else
      res.setContentType("text/html; charset=iso-8859-1");

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, infoString.length()));
  }

  static private InputStream getXSLT(String xslName) {
    Class c = FmrcInventoryServlet.class;
    return c.getResourceAsStream("/resources/xsl/" + xslName);
  }

  private String[] getDatasetPaths() {
    String[] all = {
        "/thredds/modelInventory/fmrc/NCEP/DGEX/CONUS_12km/",
        "/thredds/modelInventory/fmrc/NCEP/DGEX/Alaska_12km/",

        "/thredds/modelInventory/fmrc/NCEP/GFS/Alaska_191km/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/CONUS_80km/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/CONUS_95km/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/CONUS_191km/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/Global_0p5deg/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/Global_onedeg/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/Global_2p5deg/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/Hawaii_160km/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/N_Hemisphere_381km/",
        "/thredds/modelInventory/fmrc/NCEP/GFS/Puerto_Rico_191km/",

        "/thredds/modelInventory/fmrc/NCEP/NAM/Alaska_11km/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/Alaska_22km/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/Alaska_45km/noaaport/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/Alaska_45km/conduit/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/Alaska_95km/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_12km/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_20km/surface/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_20km/selectsurface/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_20km/noaaport/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_40km/noaaport/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_40km/conduit/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/CONUS_80km/",
        "/thredds/modelInventory/fmrc/NCEP/NAM/Polar_90km/",

        "/thredds/modelInventory/fmrc/NCEP/RUC2/CONUS_20km/surface/",
        "/thredds/modelInventory/fmrc/NCEP/RUC2/CONUS_20km/pressure/",
        "/thredds/modelInventory/fmrc/NCEP/RUC2/CONUS_20km/hybrid/",
        "/thredds/modelInventory/fmrc/NCEP/RUC2/CONUS_40km/",
        "/thredds/modelInventory/fmrc/NCEP/RUC/CONUS_80km/",

        "/thredds/modelInventory/fmrc/NCEP/NDFD/CONUS_5km/",
    };

    return all;
  }

}
