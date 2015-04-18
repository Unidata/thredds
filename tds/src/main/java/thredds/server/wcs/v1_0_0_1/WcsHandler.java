/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.wcs.v1_0_0_1;

import thredds.servlet.ServletUtil;
import thredds.server.wcs.VersionHandler;
import thredds.util.ContentType;
import thredds.util.Version;
import thredds.wcs.Request;

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
public class WcsHandler implements VersionHandler {
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger(WcsHandler.class);

  private Version version;

  public WcsHandler(String verString) {
    this.version = new Version(verString);
  }

  public Version getVersion() {
    return this.version;
  }

  public VersionHandler setDiskCache(DiskCache2 diskCache) {
    thredds.wcs.v1_0_0_1.WcsCoverage.setDiskCache(diskCache);
    return this;
  }

  private boolean deleteImmediately = true;

  public VersionHandler setDeleteImmediately(boolean deleteImmediately) {
    this.deleteImmediately = deleteImmediately;
    return this;
  }

  public void handleKVP(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException {

    thredds.wcs.v1_0_0_1.WcsRequest request = null;
    try {
      URI serverURI = new URI(req.getRequestURL().toString());
      request = WcsRequestParser.parseRequest(this.getVersion().getVersionString(), serverURI, req, res);
      if (request.getOperation().equals(Request.Operation.GetCapabilities)) {
        res.setContentType(ContentType.xml.getContentHeader());
        res.setStatus(HttpServletResponse.SC_OK);
        //res.setCharacterEncoding(CDM.UTF8);

        PrintWriter pw = res.getWriter();
        ((thredds.wcs.v1_0_0_1.GetCapabilities) request).writeCapabilitiesReport(pw);
        pw.flush();

      } else if (request.getOperation().equals(Request.Operation.DescribeCoverage)) {
        res.setContentType(ContentType.xml.getContentHeader());
        res.setStatus(HttpServletResponse.SC_OK);
        res.setCharacterEncoding(CDM.UTF8);

        PrintWriter pw = res.getWriter();
        ((thredds.wcs.v1_0_0_1.DescribeCoverage) request).writeDescribeCoverageDoc(pw);
        pw.flush();

      } else if (request.getOperation().equals(Request.Operation.GetCoverage)) {
        File covFile = ((thredds.wcs.v1_0_0_1.GetCoverage) request).writeCoverageDataToFile();
        if (covFile != null && covFile.exists()) {
          int pos = covFile.getPath().lastIndexOf(".");
          String suffix = covFile.getPath().substring(pos);
          String resultFilename = request.getDataset().getDatasetName(); // this is name browser will show
          if (!resultFilename.endsWith(suffix))
            resultFilename = resultFilename + suffix;
          res.setHeader("Content-Disposition", "attachment; filename=\"" + resultFilename + "\"");

          ServletUtil.returnFile(servlet, "", covFile.getPath(), req, res, ((thredds.wcs.v1_0_0_1.GetCoverage) request).getFormat().getMimeType());
          if (deleteImmediately) covFile.delete();
        } else {
          log.error("handleKVP(): Failed to create coverage file" + (covFile == null ? "" : (": " + covFile.getAbsolutePath())));
          throw new thredds.wcs.v1_0_0_1.WcsException("Problem creating requested coverage.");
        }
      }
    } catch (thredds.wcs.v1_0_0_1.WcsException e) {
      handleExceptionReport(res, e);

    } catch (URISyntaxException e) {
      handleExceptionReport(res, new thredds.wcs.v1_0_0_1.WcsException("Bad URI: " + e.getMessage()));

    } catch (Throwable t) {
      log.error("Unknown problem.", t);
      handleExceptionReport(res, new thredds.wcs.v1_0_0_1.WcsException("Unknown problem", t));

    } finally {
      if (request != null && request.getDataset() != null) {
        request.getDataset().close();
      }
    }
  }

  public thredds.wcs.v1_0_0_1.GetCapabilities.ServiceInfo getServiceInfo() {
    // Todo Figure out how to configure serviceId info.
    thredds.wcs.v1_0_0_1.GetCapabilities.ServiceInfo sid;
    thredds.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty respParty;
    thredds.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.ContactInfo contactInfo;
    contactInfo = new thredds.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.ContactInfo(
            Collections.singletonList("voice phone"),
            Collections.singletonList("voice phone"),
            new thredds.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.Address(
                    Collections.singletonList("address"), "city", "admin area", "postal code", "country",
                    Collections.singletonList("email")
            ),
            new thredds.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty.OnlineResource(null, "title")
    );
    respParty = new thredds.wcs.v1_0_0_1.GetCapabilities.ResponsibleParty("indiv name", "org name", "position",
            contactInfo);
    sid = new thredds.wcs.v1_0_0_1.GetCapabilities.ServiceInfo("name", "label", "description",
            Collections.singletonList("keyword"),
            respParty, "no fees",
            Collections.singletonList("no access constraints"));

    return sid;
  }

  public void handleExceptionReport(HttpServletResponse res, thredds.wcs.v1_0_0_1.WcsException exception) throws IOException {
    res.setContentType(ContentType.ogc_exception.getContentHeader());
    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

    thredds.wcs.v1_0_0_1.ExceptionReport exceptionReport = new thredds.wcs.v1_0_0_1.ExceptionReport(exception);

    PrintWriter pw = res.getWriter();
    exceptionReport.writeExceptionReport(pw);
    pw.flush();
  }

  public void handleExceptionReport(HttpServletResponse res, String code, String locator, String message) throws IOException {
    thredds.wcs.v1_0_0_1.WcsException.Code c;
    thredds.wcs.v1_0_0_1.WcsException exception;
    try {
      c = thredds.wcs.v1_0_0_1.WcsException.Code.valueOf(code);
      exception = new thredds.wcs.v1_0_0_1.WcsException(c, locator, message);
    } catch (IllegalArgumentException e) {
      exception = new thredds.wcs.v1_0_0_1.WcsException(message);
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
