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


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.*;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jdom.transform.XSLTransformer;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.MDC;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

/**
 * Servlet handles CDM Coordinate System validation.
 * @author caron
 */
public class CoordSysValidatorServlet extends AbstractServlet {
  private DiskCache2 cdmValidateCache = null;
  private DiskFileItemFactory factory;
  private File cacheDir;
  private long maxFileUploadSize;
  private boolean allow = false, deleteImmediately = true;

  public void init() throws ServletException {
    super.init();

    /*   <CdmValidatorService>
    <allow>true</allow>
    <dir>/temp/vcache/</dir>
    <maxFileUploadSize>1 Gb</maxFileUploadSize>
  </CdmValidatorService>
  */

    allow = ThreddsConfig.getBoolean("CdmValidatorService.allow", false);
    maxFileUploadSize = ThreddsConfig.getBytes("CdmValidatorService.maxFileUploadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("CdmValidatorService.dir", contentPath);

    int scourSecs = ThreddsConfig.getSeconds("CdmValidatorService.scour", -1);
    int maxAgeSecs = ThreddsConfig.getSeconds("CdmValidatorService.maxAge", -1);
    if (maxAgeSecs > 0) {
      deleteImmediately = false;
      cdmValidateCache = new DiskCache2(cache, false, maxAgeSecs/60, scourSecs/60);
    }

    cacheDir = new File(cache);
    cacheDir.mkdirs();
    factory = new DiskFileItemFactory(0, cacheDir); // LOOK can also do in-memory
  }

  public void destroy() {
    if (cdmValidateCache != null)
      cdmValidateCache.exit();
    super.destroy();
  }

  protected String getPath() { return "cdmValidate/"; }
  protected void makeDebugActions() { }

  /**
   * GET handles the case where its a remote URL (dods or http)
   * @param req  request
   * @param res  response
   * @throws ServletException
   * @throws IOException
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    log.info( UsageLog.setupRequestContext(req));

    String urlString = req.getParameter("URL");
    if (urlString == null) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must have a URL parameter");
      return;
    }

    // validate the uri String
    try {
      URI uri = new URI(urlString);
      urlString = uri.toASCIIString(); // LOOK do we want just toString() ? Is this useful "input validation" ?
    } catch (URISyntaxException e) {
       ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
       res.sendError(HttpServletResponse.SC_BAD_REQUEST, "URISyntaxException on URU parameter");
       return;
    }

    String xml = req.getParameter("xml");
    boolean wantXml = (xml != null) && xml.equals("true");

    try {
      int len = showValidatorResults(res, urlString, wantXml);
      log.info( "URL = " + urlString);
      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, len);

    } catch (Exception e) {
      log.error("Validator internal error", e);
      ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validator internal error");
    }

  }

  /**
   * POST handles uploaded files
   * @param req  request
   * @param res response
   * @throws ServletException
   * @throws IOException
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    log.info( UsageLog.setupRequestContext(req));

    // Check that we have a file upload request
    boolean isMultipart = ServletFileUpload.isMultipartContent(req);
    if (!isMultipart) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    //Create a new file upload handler
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(maxFileUploadSize);  // maximum bytes before a FileUploadException will be thrown

    List<FileItem> fileItems;
    try {
      fileItems = (List<FileItem>) upload.parseRequest(req);
    }
    catch (FileUploadException e) {
      log.info("Validator FileUploadException", e);
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    //Process the uploaded items
    String username = null;
    boolean wantXml = false;
    for (FileItem item : fileItems) {
      if (item.isFormField()) {
        if ("username".equals(item.getFieldName()))
          username = item.getString();
        if ("xml".equals(item.getFieldName()))
          wantXml = item.getString().equals("true");
      }
    }

    for (FileItem item : fileItems) {
      if (!item.isFormField()) {
        try {
          processUploadedFile(req, res, (DiskFileItem) item, username, wantXml);
          return;
        } catch (Exception e) {
          log.info("Validator processUploadedFile", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
      }
    }

  }

  private void processUploadedFile(HttpServletRequest req, HttpServletResponse res, DiskFileItem item,
          String username, boolean wantXml) throws Exception {

    if ((username == null) || (username.length() == 0))
      username = "none";
    username = StringUtil.filter(username, "_");
    String filename = item.getName();
    filename = StringUtil.replace(filename, "/","-");
    filename = StringUtil.filter(filename, ".-_");

    File uploadedFile = new File(cacheDir+"/"+username + "/"+ filename);
    uploadedFile.getParentFile().mkdirs();
    item.write(uploadedFile);

    int len = showValidatorResults(res, uploadedFile.getPath(), wantXml);

    if (deleteImmediately) {
      try {
        uploadedFile.delete();
      } catch (Exception e) {
        log.error( "Uploaded File = " + uploadedFile.getPath() + " delete failed = " + e.getMessage());
      }
    }

    if (req.getRemoteUser() == null) {
      if (username != null)
        MDC.put( "userid", username);
    }

    log.info( "Uploaded File = " + item.getName() + " sent to " + uploadedFile.getPath() + " size= " + uploadedFile.length());
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, len);
  }

  private int showValidatorResults(HttpServletResponse res, String location, boolean wantXml) throws Exception {

    NetcdfDatasetInfo info = null;
    try {
      info = new NetcdfDatasetInfo( location);

    String infoString;

    if (wantXml) {
      infoString = info.writeXML();
      res.setContentLength(infoString.length());
      res.setContentType("text/xml; charset=iso-8859-1");

    } else {
      Document xml = info.makeDocument();
      InputStream is = getXSLT();
      XSLTransformer transformer = new XSLTransformer(is);

      Document html = transformer.transform(xml);
      XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
      infoString = fmt.outputString( html);

      res.setContentType("text/html; charset=iso-8859-1");
    }

    res.setContentLength(infoString.length());

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    return infoString.length();

    } finally {
      if (null != info)
        try {
          info.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + location);
        }
    }
  }

  private InputStream getXSLT() {
    Class c = CoordSysValidatorServlet.class;
    String resource = "/WEB-INF/classes/resources/xsl/cdmValidation.xsl";
    InputStream is = c.getResourceAsStream(resource);
    if (null == is)
      log.error( "Cant load XSLT resource = " + resource);

    return is;
  }
}

