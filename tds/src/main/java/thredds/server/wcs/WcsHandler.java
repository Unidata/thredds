/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs;

import thredds.servlet.ServletUtil;
import thredds.util.ContentType;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import ucar.nc2.constants.CDM;
import ucar.nc2.util.DiskCache2;

/**
 * WCS 1.0 handler
 *
 * @author edavis
 * @since 4.0
 */
public class WcsHandler {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WcsHandler.class);

  private Version version;

  public WcsHandler(String verString) {
    this.version = new Version(verString);
  }

  public Version getVersion() {
    return this.version;
  }

  public WcsHandler setDiskCache(DiskCache2 diskCache) {
    thredds.server.wcs.v1_0_0_1.WcsCoverage.setDiskCache(diskCache);
    return this;
  }

  private boolean deleteImmediately = true;

  public WcsHandler setDeleteImmediately(boolean deleteImmediately) {
    this.deleteImmediately = deleteImmediately;
    return this;
  }

  public void handleKVP(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException {

    thredds.server.wcs.v1_0_0_1.WcsRequest request = null;
    try {
      URI serverURI = new URI(req.getRequestURL().toString());
      request = WcsRequestParser.parseRequest(this.getVersion().getVersionString(), serverURI, req, res);
      if (request == null) return;

      if (request.getOperation().equals(Request.Operation.GetCapabilities)) {
        res.setContentType(ContentType.xml.getContentHeader());
        res.setStatus(HttpServletResponse.SC_OK);
        //res.setCharacterEncoding(CDM.UTF8);

        PrintWriter pw = res.getWriter();
        ((thredds.server.wcs.v1_0_0_1.GetCapabilities) request).writeCapabilitiesReport(pw);
        pw.flush();

      } else if (request.getOperation().equals(Request.Operation.DescribeCoverage)) {
        res.setContentType(ContentType.xml.getContentHeader());
        res.setStatus(HttpServletResponse.SC_OK);
        res.setCharacterEncoding(CDM.UTF8);

        PrintWriter pw = res.getWriter();
        ((thredds.server.wcs.v1_0_0_1.DescribeCoverage) request).writeDescribeCoverageDoc(pw);
        pw.flush();

      } else if (request.getOperation().equals(Request.Operation.GetCoverage)) {
        File covFile = ((thredds.server.wcs.v1_0_0_1.GetCoverage) request).writeCoverageDataToFile();
        if (covFile != null && covFile.exists()) {
          int pos = covFile.getPath().lastIndexOf(".");
          String suffix = covFile.getPath().substring(pos);
          String resultFilename = request.getWcsDataset().getDatasetName(); // this is name browser will show
          if (!resultFilename.endsWith(suffix))
            resultFilename = resultFilename + suffix;
          res.setHeader("Content-Disposition", "attachment; filename=\"" + resultFilename + "\"");

          ServletUtil.returnFile(servlet, "", covFile.getPath(), req, res, ((thredds.server.wcs.v1_0_0_1.GetCoverage) request).getFormat().getMimeType());
          if (deleteImmediately) covFile.delete();
        } else {
          log.error("handleKVP(): Failed to create coverage file" + (covFile == null ? "" : (": " + covFile.getAbsolutePath())));
          throw new thredds.server.wcs.v1_0_0_1.WcsException("Problem creating requested coverage.");
        }
      }
    } catch (thredds.server.wcs.v1_0_0_1.WcsException e) {
      handleExceptionReport(res, e);

    } catch (URISyntaxException e) {
      handleExceptionReport(res, new thredds.server.wcs.v1_0_0_1.WcsException("Bad URI: " + e.getMessage()));

    } catch (Throwable t) {
      log.error("Unknown problem.", t);
      handleExceptionReport(res, new thredds.server.wcs.v1_0_0_1.WcsException("Unknown problem", t));

    } finally {
      if (request != null && request.getWcsDataset() != null) {
        request.getWcsDataset().close();
      }
    }
  }

  public thredds.server.wcs.v1_0_0_1.GetCapabilities.ServiceInfo getServiceInfo() {
    // Todo Figure out how to configure serviceId info.
    thredds.server.wcs.v1_0_0_1.GetCapabilities.ServiceInfo sid;
    thredds.server.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty respParty;
    thredds.server.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.ContactInfo contactInfo;
    contactInfo = new thredds.server.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.ContactInfo(
            Collections.singletonList("voice phone"),
            Collections.singletonList("voice phone"),
            new thredds.server.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.Address(
                    Collections.singletonList("address"), "city", "admin area", "postal code", "country",
                    Collections.singletonList("email")
            ),
            new thredds.server.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.OnlineResource(null, "title")
    );
    respParty = new thredds.server.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty("indiv name", "org name", "position",
            contactInfo);
    sid = new thredds.server.wcs.v1_0_0_1.GetCapabilities.ServiceInfo("name", "label", "description",
            Collections.singletonList("keyword"),
            respParty, "no fees",
            Collections.singletonList("no access constraints"));

    return sid;
  }

  public void handleExceptionReport(HttpServletResponse res, thredds.server.wcs.v1_0_0_1.WcsException exception) throws IOException {
    res.setContentType(ContentType.ogc_exception.getContentHeader());
    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

    thredds.server.wcs.v1_0_0_1.ExceptionReport exceptionReport = new thredds.server.wcs.v1_0_0_1.ExceptionReport(exception);

    PrintWriter pw = res.getWriter();
    exceptionReport.writeExceptionReport(pw);
    pw.flush();
  }

  public void handleExceptionReport(HttpServletResponse res, String code, String locator, String message) throws IOException {
    thredds.server.wcs.v1_0_0_1.WcsException.Code c;
    thredds.server.wcs.v1_0_0_1.WcsException exception;
    try {
      c = thredds.server.wcs.v1_0_0_1.WcsException.Code.valueOf(code);
      exception = new thredds.server.wcs.v1_0_0_1.WcsException(c, locator, message);
    } catch (IllegalArgumentException e) {
      exception = new thredds.server.wcs.v1_0_0_1.WcsException(message);
      log.debug("handleExceptionReport(): bad code given [" + code + "].");
    }

    handleExceptionReport(res, exception);
  }

  public void handleExceptionReport(HttpServletResponse res, String code, String locator, Throwable t) throws IOException {
    handleExceptionReport(res, code, locator, t.getMessage());

    if (t instanceof FileNotFoundException)
      log.info("handleExceptionReport", t.getMessage()); // dont clutter up log files
    else
      log.info("handleExceptionReport", t);
  }

}
