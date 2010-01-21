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
import thredds.server.config.TdsContext;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.DatasetHandler;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
import thredds.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.controller.ColorScaleRange;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.exceptions.Wms1_1_1Exception;
import uk.ac.rdg.resc.ncwms.config.Config;

/**
 * Controller for WMS service
 * User: pmak
 * Date: Aug 3, 2008
 * Time: 2:04:38 PM
 * <p/>
 */
public class WMSController extends AbstractController {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WMSController.class);
  private static org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");

  public static final Version WMS_VER_1_1_1 = new Version("1.1.1");
  public static final Version WMS_VER_1_3_0 = new Version("1.3.0");

  private static final Set<NetcdfDataset.Enhance> enhanceMode = EnumSet.of(NetcdfDataset.Enhance.ScaleMissingDefer, NetcdfDataset.Enhance.CoordSystems);

  private boolean allow;
  private boolean allowRemote;

  protected String getPath() {
    return "wms/";
  }

  private Config config;
  private Map<String, ColorScaleRange> colorRange;

  public void init() throws ServletException {
    logServerStartup.info( "WMS - initialization start: " + UsageLog.setupNonRequestContext() );

    allow = ThreddsConfig.getBoolean("WMS.allow", false);
    allowRemote = ThreddsConfig.getBoolean("WMS.allowRemote", false);

    if (allow) {
      // OGC metadata
      String defaultOgcMetaXmlFilePath = this.getServletContext().getRealPath("/WEB-INF/OGCMeta.xml");
      String ogcMetaXmlFilePath = ThreddsConfig.get("WMS.ogcMetaXML", null);
      if ( ogcMetaXmlFilePath == null)
        ogcMetaXmlFilePath = defaultOgcMetaXmlFilePath;
      else if (!ogcMetaXmlFilePath.startsWith("/"))
        ogcMetaXmlFilePath = tdsContext.getContentDirectory().getPath() + "/" + ogcMetaXmlFilePath;

      try {
        config = new Persister().read(Config.class, new File( ogcMetaXmlFilePath ));
        logServerStartup.debug("Loaded OGCMetaXmlFile from " + ogcMetaXmlFilePath );
      }
      catch (Exception e) {
        logServerStartup.error("Failed to load OGCMetaXmlFile from " + ogcMetaXmlFilePath );
        logServerStartup.info( "WMS - initialization done: " + UsageLog.closingMessageNonRequestContext() );
        throw new ServletException("Cannot read OGC config file " + e.toString());
      }

      // color pallettes
      String paletteLocation = ThreddsConfig.get( "WMS.paletteLocationDir",
                                                  this.getServletContext().getRealPath( "/WEB-INF/palettes" ));
      if ( paletteLocation == null )
      {
        // Default palette directory not found!!!
        allow = allowRemote = false;
        logServerStartup.error( "Palette location not configured and default location not found." +
                                "\n**** Disabling WMS - check palette configuration: "
                                + UsageLog.closingMessageNonRequestContext() );
        return;
      }
      File paletteLocationDir = new File( paletteLocation );
      if ( ! paletteLocationDir.isAbsolute())
      {
        paletteLocationDir = tdsContext.getConfigFileSource().getFile( paletteLocation );
        if ( paletteLocationDir == null )
        {
          // User configured palette directory not found!!!
          allow = allowRemote = false;
          logServerStartup.error( "Palette location [" + paletteLocation + "] not found." +
                                  "\n**** Disabling WMS - check palette configuration: "
                                  + UsageLog.closingMessageNonRequestContext() );
          return;
        }
      }

      if ( paletteLocationDir.exists() && paletteLocationDir.isDirectory() )
      {
        ColorPalette.loadPalettes( paletteLocationDir );
        logServerStartup.debug( "Loaded palettes from " + paletteLocation );
      }
      else
      {
        // Palette directory doesn't exist or isn't directory!!!
        allow = allowRemote = false;
        logServerStartup.error( "Palette location directory [" + paletteLocation + "] doesn't exist or isn't a directory." +
                                "\n**** Disabling WMS - check palette configuration: "
                                + UsageLog.closingMessageNonRequestContext() );
        return;
//        logServerStartup.warn( "Directory of palette files does not exist or is not a directory.  paletteLocation=" + paletteLocation );
//        ColorPalette.loadPalettes( paletteLocationDir );
      }

      colorRange = new HashMap<String, ColorScaleRange>();

      // LOOK Problem - global setting
      //NetcdfDataset.setDefaultEnhanceMode(enhanceMode);
    }

    logServerStartup.info( "WMS - initialization done: " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy() {
    logServerStartup.info( "WMS - destroy start: " + UsageLog.setupNonRequestContext() );
    NetcdfDataset.shutdown();
    logServerStartup.info( "WMS - destroy done: " + UsageLog.closingMessageNonRequestContext() );
  }

  private TdsContext tdsContext;

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    String jspPage = "";
    ModelAndView result = null;

    log.info(UsageLog.setupRequestContext(req));

    if (!allow) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "WMS service not supported.");
      return null;
    }

    // Check if the request is for a remote dataset; if it is only
    // proceed if the TDS is configured to allow it.
    String datasetURL = ServletUtil.getParameterIgnoreCase(req, "dataset");
    // ToDo LOOK - move this into TdsConfig?
    if (datasetURL != null && !allowRemote) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "WMS service not supported for remote datasets.");
      return null;
    }

    Map<String, Object> model = new HashMap<String, Object>();
    UsageLogEntry usageLogEntry = new UsageLogEntry(req);
    GridDataset dataset = null;
    String errMessage = "";

    RequestParams params = new RequestParams(req.getParameterMap());
    String versionString = params.getWmsVersion();

    try {
      if (versionString == null) {
        //for Google!
        versionString = "1.1.1";
      }

      String request = params.getMandatoryString("request");
      dataset = openDataset(req, res);
      FileBasedResponse response;

      log.debug("Processing request: (version): " + versionString);

      if (request.equalsIgnoreCase("GetCapabilities")) {
        String service = params.getMandatoryString("service");
        if (!service.equalsIgnoreCase("WMS"))
          throw new WmsException("The SERVICE parameter must be WMS");

        errMessage = "Error encountered while processing GetCapabilities request";
        long startupDate = this.getApplicationContext().getStartupDate();
        GetCapabilities getCap = new GetCapabilities(params, dataset, usageLogEntry);
        getCap.setConfig(config);
        getCap.setStartupDate(startupDate);
        response = getCap;

      } else if (request.equalsIgnoreCase("GetMap")) {
        errMessage = "Error encountered while processing GetMap request ";
        response = new WmsGetMap(params, dataset, usageLogEntry);

      } else if (request.equalsIgnoreCase("GetLegendGraphic")) {
        errMessage = "Error encountered while processing GetLegendGraphic request ";
        response = new GetLegendGraphic(params, dataset, usageLogEntry);

      } else if (request.equalsIgnoreCase("GetFeatureInfo")) {
        errMessage = "Error encountered while processing GetFeatureInfo request ";
        response = new GetFeatureInfo(params, dataset, usageLogEntry);

      } else if (request.equals("GetMetadata")) {
        errMessage = "Error encountered while processing GetMetadata request ";
        MetadataResponse metaController = new MetadataResponse(params, dataset, usageLogEntry);
        metaController.setConfig(config);
        response = metaController;
      }
//        else if (request.equals("GetKML")) {
//
//        } else if (request.equals("GetKMLRegion")) {
//
//        }
      else
        throw new WmsException("Unsupported REQUEST parameter="+request);

      result = response.processRequest(res, req);
      closeDataset(dataset);

      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
      return result;

    } catch (MetadataException me) {
      log.debug("MetadataException: " + me.toString());
      // falls through

    } catch (WmsException e) {
      log.debug("WMS Exception! " + errMessage);
      if (versionString.equals("1.1.1")) {
        model.put("exception", new Wms1_1_1Exception(e));
        jspPage = "displayWms1_1_1Exception";
      } else if (versionString.equals("1.3.0")) {
        model.put("exception", e);
        jspPage = "displayWmsException";
      } else {
        model.put("exception", e);
        jspPage = "displayWmsException";
      }

      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      return new ModelAndView(jspPage, model);

    } catch (java.net.SocketException se) { // Google Earth does this a lot for some reason
      log.info(UsageLog.closingMessageForRequestContext(ServletUtil.STATUS_CLIENT_ABORT, -1), se);
      return null;

    } catch (IOException e) {
      String eName = e.getClass().getName(); // dont want compile-time dependency on ClientAbortException
      if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
        log.info("ClientAbortException: " + e.getMessage());
        log.info(UsageLog.closingMessageForRequestContext(ServletUtil.STATUS_CLIENT_ABORT, -1));
        return null;
      }
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1), e);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;

    } catch (Throwable t) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1), t);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;

    } finally {
      //if ((result == null) || (result.getModel() == null) || (result.getModel().get("dataset") == null)) {
      closeDataset(dataset);
      // } // else use DatasetCloser HandlerInterceptor
    }

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
    return null;
  }

  //Clearly, this is something STOLEN from WCSServlet
  private GridDataset openDataset(HttpServletRequest req, HttpServletResponse res) throws WmsException {
    log.debug("in openDataset");
    GridDataset dataset;
    String datasetPath = req.getPathInfo();

    boolean isRemote = false;

    if (datasetPath == null) { // passing in a dataset URL, presumably opendap
      datasetPath = ServletUtil.getParameterIgnoreCase(req, "dataset");
      isRemote = (datasetPath != null);
    }

    try {
      // ope ndirectly if an opendap URL, else go through DatasetHandler
      dataset = isRemote ? ucar.nc2.dt.grid.GridDataset.open(datasetPath, enhanceMode) : DatasetHandler.openGridDataset(req, res, datasetPath, enhanceMode);

    } catch (IOException e) {
      log.info("WMSController: Failed to open dataset <" + datasetPath + ">: " + e.getMessage());
      throw new WmsException("Failed to open dataset, \"" + datasetPath + "\".");
    }

    if (dataset == null) {
      log.debug("WMSController: Unknown dataset <" + datasetPath + ">.");
      throw new WmsException("Unknown dataset, \"" + datasetPath + "\".");
    }

    log.debug("leave openDataset");
    return dataset;
  }

  private void closeDataset(GridDataset dataset) {
    if (dataset == null) return;
    try {
      //System.out.println("**Controller closed "+dataset.getLocationURI());
      dataset.close();
    } catch (IOException ioe) {
      log.warn("Failed to properly close the dataset", ioe);
    }
  }
}
