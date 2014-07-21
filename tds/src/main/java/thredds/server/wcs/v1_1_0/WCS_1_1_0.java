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
package thredds.server.wcs.v1_1_0;

import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import thredds.util.Version;
import thredds.server.wcs.VersionHandler;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import ucar.nc2.constants.CDM;
import ucar.nc2.util.DiskCache2;

/**
 * Handles WCS 1.1.0
 *
 * @author edavis
 * @since 4.0
 */
public class WCS_1_1_0 implements VersionHandler {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WCS_1_1_0.class);

  private Version version;

  /**
   * Declare the default constructor to be package private.
   */
  WCS_1_1_0(String versionString) {
    this.version = new Version(versionString);
  }

  public Version getVersion() {
    return this.version;
  }

  public VersionHandler setDiskCache(DiskCache2 diskCache) {
    thredds.wcs.v1_1_0.GetCoverage.setDiskCache(diskCache);
    return this;
  }

  private boolean deleteImmediately = true;

  public VersionHandler setDeleteImmediately(boolean deleteImmediately) {
    this.deleteImmediately = deleteImmediately;
    return this;
  }

  public void handleKVP(HttpServlet servlet2, HttpServletRequest req, HttpServletResponse res) throws IOException  {

    try {
      URI serverURI = new URI(req.getRequestURL().toString());
      thredds.wcs.v1_1_0.Request request = WcsRequestParser.parseRequest(this.getVersion().getVersionString(), req, res);
      if (request.getOperation().equals(thredds.wcs.v1_1_0.Request.Operation.GetCapabilities)) {
        thredds.wcs.v1_1_0.GetCapabilities getCapabilities =
                new thredds.wcs.v1_1_0.GetCapabilities(serverURI, request.getSections(),
                        getServiceId(), getServiceProvider(),
                        request.getDataset());
        res.setContentType(ContentType.xml.getContentHeader());
        res.setStatus(HttpServletResponse.SC_OK);

        PrintWriter pw = res.getWriter();
        getCapabilities.writeCapabilitiesReport(pw);
        pw.flush();

      } else if (request.getOperation().equals(thredds.wcs.v1_1_0.Request.Operation.DescribeCoverage)) {
        thredds.wcs.v1_1_0.DescribeCoverage descCoverage =
                new thredds.wcs.v1_1_0.DescribeCoverage(serverURI, request.getIdentifierList(),
                        request.getDataset());
        res.setContentType(ContentType.xml.getContentHeader());
        res.setStatus(HttpServletResponse.SC_OK);
        res.setCharacterEncoding(CDM.UTF8);

        PrintWriter pw = res.getWriter();
        descCoverage.writeDescribeCoverageDoc(pw);
        pw.flush();

      } else if (request.getOperation().equals(thredds.wcs.v1_1_0.Request.Operation.GetCoverage)) {
        // ToDo Handle multi-part MIME response
        thredds.wcs.v1_1_0.GetCoverage getCoverage =
                new thredds.wcs.v1_1_0.GetCoverage(serverURI, request.getIdentifier(),
                        request.getDatasetPath(),
                        request.getDataset());
        File covFile = getCoverage.writeCoverageDataToFile();
        if (covFile != null && covFile.exists()) {
          int pos = covFile.getPath().lastIndexOf(".");
          String suffix = covFile.getPath().substring(pos);
          String resultFilename = request.getDatasetName(); // this is name browser will show
          if (!resultFilename.endsWith(suffix))
            resultFilename = resultFilename + suffix;
          res.setHeader("Content-Disposition", "attachment; filename=\"" + resultFilename + "\"");

          ServletUtil.returnFile(null, "", covFile.getPath(), req, res, "application/netcdf");
          if (deleteImmediately) covFile.delete();

        } else {
          log.error("handleKVP(): Failed to create coverage file" + (covFile == null ? "" : (": " + covFile.getAbsolutePath())));
          throw new thredds.wcs.v1_1_0.WcsException("Problem creating requested coverage.");
        }
      }

    } catch (thredds.wcs.v1_1_0.WcsException e) {
      handleExceptionReport(res, e);

    } catch (URISyntaxException e) {
      handleExceptionReport(res, new thredds.wcs.v1_1_0.WcsException("Bad URI: " + e.getMessage()));

    } catch (Throwable t) {
      log.error("Unknown problem.", t);
      handleExceptionReport(res, new thredds.wcs.v1_1_0.WcsException("Unknown problem", t));
    }
  }

  private thredds.wcs.v1_1_0.GetCapabilities.ServiceId getServiceId() {
    // Todo Figure out how to configure serviceId info.
    thredds.wcs.v1_1_0.GetCapabilities.ServiceId sid;
    sid = new thredds.wcs.v1_1_0.GetCapabilities.ServiceId("title", "abstract",
            Collections.singletonList("keyword"),
            "WCS", Collections.singletonList("1.1.0"),
            "", Collections.singletonList(""));

    return sid;
  }

  private thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider getServiceProvider() {
    // Todo Figure out how to configure serviceProvider info.
    thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.OnlineResource resource =
            new thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.OnlineResource(null, "a link");
    thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.Address address = null; //new GetCapabilities.ServiceProvider.Address(...);
    List<String> phone = Collections.emptyList();
    List<String> fax = Collections.emptyList();
    thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.ContactInfo contactInfo =
            new thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.ContactInfo(phone, fax, address, null, "hours", "contact instructions");
    thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.ServiceContact contact =
            new thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider.ServiceContact("individual name", "position name", contactInfo, "role");

    return new thredds.wcs.v1_1_0.GetCapabilities.ServiceProvider("sp name", resource, contact);
  }

  public void handleExceptionReport(HttpServletResponse res, thredds.wcs.v1_1_0.WcsException exception)
          throws IOException {
    res.setContentType(ContentType.xml.getContentHeader()); // 1.0.0 was ("application/vnd.ogc.se_xml" );
    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

    thredds.wcs.v1_1_0.ExceptionReport exceptionReport = new thredds.wcs.v1_1_0.ExceptionReport(exception);

    PrintWriter pw = res.getWriter();
    exceptionReport.writeExceptionReport(pw);
    pw.flush();
  }

  public void handleExceptionReport(HttpServletResponse res, String code, String locator, String message)
          throws IOException {
    thredds.wcs.v1_1_0.WcsException.Code c;
    thredds.wcs.v1_1_0.WcsException exception;
    try {
      c = thredds.wcs.v1_1_0.WcsException.Code.valueOf(code);
      exception = new thredds.wcs.v1_1_0.WcsException(c, locator, message);
    } catch (IllegalArgumentException e) {
      exception = new thredds.wcs.v1_1_0.WcsException(message);
      log.debug("handleExceptionReport(): bad code given <" + code + ">.");
    }

    handleExceptionReport(res, exception);
  }

  public void handleExceptionReport(HttpServletResponse res, String code, String locator, Throwable t)
          throws IOException {
    handleExceptionReport(res, code, locator, t.getMessage());

    if (t instanceof FileNotFoundException)
      log.info("handleExceptionReport", t.getMessage()); // dont clutter up log files
    else
      log.info("handleExceptionReport", t);
  }

}
