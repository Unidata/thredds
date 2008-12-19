/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
import org.apache.log4j.MDC;
import org.jdom.transform.XSLTransformer;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
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

    ServletUtil.logServerAccessSetup(req);

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

    ServletUtil.logServerAccessSetup(req);

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

