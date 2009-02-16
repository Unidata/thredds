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
package thredds.server.wms;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.simpleframework.xml.load.Persister;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import thredds.server.wms.responses.*;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.DatasetHandler;
import thredds.servlet.UsageLog;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.controller.ColorScaleRange;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.config.Config;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: Aug 3, 2008
 * Time: 2:04:38 PM
 * <p/>
 * To change this template use File | Settings | File Templates.
 */
public class WMSController extends AbstractController {
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger(WMSController.class);

  private boolean allow;

  protected String getPath() {
    return "wms/";
  }

  protected void makeDebugActions() {
  }

  private Config config;
  private Map<String, ColorScaleRange> colorRange;

  public void init() throws ServletException {
    allow = ThreddsConfig.getBoolean("WMS.allow", false);
    log.info("initializing WMS: " + allow);

    if (allow) {
      String paletteLocation = this.getServletContext().getRealPath("/WEB-INF/" +
              ThreddsConfig.get("WMS.paletteLocationDir", "palettes"));
      String OGCMetaXmlFile = this.getServletContext().getRealPath("/WEB-INF/" +
              ThreddsConfig.get("WMS.ogcMetaXML", "OGCMeta.xml"));

      File configFile = null;
      try {
        configFile = new File(OGCMetaXmlFile);
        config = new Persister().read(Config.class, configFile);
      }
      catch (Exception e) {
        log.debug("Loaded configuration from " + OGCMetaXmlFile);
        throw new ServletException("Cannot read OGC config file " + e.toString());
      }

      File paletteLocationDir = new File(paletteLocation);
      if (paletteLocationDir.exists() && paletteLocationDir.isDirectory()) {
        ColorPalette.loadPalettes(paletteLocationDir);
      } else {
        log.info("Directory of palette files does not exist or is not a directory");
      }

      colorRange = new HashMap<String, ColorScaleRange>();

      // LOOK Problem - global setting
      NetcdfDataset.setDefaultEnhanceMode(EnumSet.of(NetcdfDataset.Enhance.ScaleMissingDefer, NetcdfDataset.Enhance.CoordSystems));
    }
  }

  public void destroy() {
    NetcdfDataset.shutdown();
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException, Exception {
    String jspPage = "";

    if (allow) {
      Map<String, Object> model = new HashMap<String, Object>();
      UsageLogEntry usageLogEntry = new UsageLogEntry(req);
      GridDataset dataset = null;
      String errMessage = "";

      RequestParams params = new RequestParams(req.getParameterMap());
      String version = params.getWmsVersion();

      if (version == null) {
        //for Google!
        version = "1.1.1";
      }

      UsageLog.log.info(UsageLog.setupRequestContext(req));

      try {
        String request = params.getMandatoryString("request");
        dataset = openDataset(req, res);

        log.debug("Processing request: (version): " + version);

        if (request.equalsIgnoreCase("GetCapabilities")) {
          errMessage = "Error encountered while processing GetCapabilities request";
          GetCapabilities getCap = new GetCapabilities(params, dataset, usageLogEntry);
          getCap.setConfig(config);
          return getCap.processRequest(res, req);

        } else if (request.equalsIgnoreCase("GetMap")) {
          errMessage = "Error encountered while processing GetMap request ";
          WmsGetMap getMapHandler = new WmsGetMap(params, dataset, usageLogEntry);
          UsageLog.log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
          return getMapHandler.processRequest(res, req);

        } else if (request.equalsIgnoreCase("GetLegendGraphic")) {
          errMessage = "Error encountered while processing GetMap request ";
          GetLegendGraphic legendGraphic = new GetLegendGraphic(params, dataset, usageLogEntry);
          return legendGraphic.processRequest(res, req);

        } else if (request.equalsIgnoreCase("GetFeatureInfo")) {
          errMessage = "Error encountered while processing GetMap request ";
          GetFeatureInfo featureInfoHandler = new GetFeatureInfo(params, dataset, usageLogEntry);
          return featureInfoHandler.processRequest(res, req);

        } else if (request.equals("GetMetadata")) {
          MetadataResponse metaController = new MetadataResponse(params, dataset, usageLogEntry);
          metaController.setConfig(config);
          return metaController.processRequest(res, req);

        } else if (request.equals("GetKML")) {

        } else if (request.equals("GetKMLRegion")) {

        }
      }
      catch (MetadataException me) {
        log.debug("MetadataException: " + me.toString());
      }
      catch (WmsException e) {
        model.put("exception", e);
        log.debug("WMS Exception!~!!! " + errMessage);
        if (version.equals("1.1.1")) {
          jspPage = "displayWms1_1_1Exception";
        } else if (version.equals("1.3.0")) {
          jspPage = "displayWmsException";
        }

        model.put("exception", e);
        return new ModelAndView(jspPage, model);
      }
      catch (java.net.SocketException se) { // Google Earth does thius a lot for some reason
        UsageLog.log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -10));
        return null;
      }
      catch (Throwable t) {
        UsageLog.log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1));
        t.printStackTrace();
        throw new RuntimeException(t);
      }
      finally {
        if (dataset != null) {
          dataset.close();
        }
      }
    } else {
      // ToDo - Server not configured to support WMS. Should
      // response code be 404 (Not Found) instead of 403 (Forbidden)?
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
    }

    UsageLog.log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -3));
    return null;
  }

  //Clearly, this is something STOLEN from WCSServlet
  private GridDataset openDataset(HttpServletRequest req, HttpServletResponse res) throws WmsException {
    log.debug("in openDataset");
    GridDataset dataset;
    String datasetPath = req.getPathInfo();
    try {
      dataset = DatasetHandler.openGridDataset(req, res, datasetPath);
    }
    catch (IOException e) {
      log.warn("WMSController: Failed to open dataset <" + datasetPath + ">: " + e.getMessage());
      throw new WmsException("Failed to open dataset, \"" + datasetPath + "\".");
    }

    if (dataset == null) {
      log.debug("WMSController: Unknown dataset <" + datasetPath + ">.");
      throw new WmsException("Unknown dataset, \"" + datasetPath + "\".");
    }

    log.debug("leave openDataset");
    return dataset;
  }
}
