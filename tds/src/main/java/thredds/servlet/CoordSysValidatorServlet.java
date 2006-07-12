// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import java.util.Iterator;

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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

/**
 * Servlet handles CDM Coordinate System validation.
 * @author caron
 * @version $Revision$ $Date$
 */
public class CoordSysValidatorServlet extends AbstractServlet {
  private DiskCache2 cdmValidateCache = null;
  private DiskFileItemFactory factory;
  private File cacheDir;

  public void init() throws ServletException {
    super.init();

    String cache = ServletParams.getInitParameter("CdmValidatorCachePath", contentPath);

    cacheDir = new File(cache);
    cacheDir.mkdirs();
    factory = new DiskFileItemFactory(0, cacheDir); // LOOK can also do in-memory

    // every 24 hours, delete stuff older than 30 days
    cdmValidateCache = new DiskCache2(cache, false, 60 * 24 * 30, 60 * 24);
    cdmValidateCache.setLogger( org.slf4j.LoggerFactory.getLogger("cacheLogger"));
  }

  public void destroy() {
    cdmValidateCache.exit();
  }

  protected String getPath() { return "cdmValidate/"; }
  protected void makeDebugActions() { }

  /**
   * GET handles the case whete its a remote URL (dods or http)
   * @param req  request
   * @param res  response
   * @throws ServletException
   * @throws IOException
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {

    ServletUtil.logServerAccessSetup(req);

    String urlString = req.getParameter("URL");
    if (urlString == null) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must have a URL parameter");
      return;
    }

    String xml = req.getParameter("xml");
    boolean wantXml = (xml != null) && xml.equals("true");

    NetcdfDataset ncd;
    try {
      ncd = NetcdfDataset.openDataset(urlString, true, null);

    } catch (IOException e) {
      log.info("Cant open url "+urlString, e);
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }

    try {
      int len = showValidatorResults(res, ncd, wantXml);
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
    upload.setSizeMax(100 * 1000 * 1000);  // maximum bytes before a FileUploadException will be thrown

    List fileItems;
    try {
      fileItems = upload.parseRequest(req);
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
    Iterator iter = fileItems.iterator();
    while (iter.hasNext()) {
      FileItem item = (FileItem) iter.next();

      if (item.isFormField()) {
        if ("username".equals(item.getFieldName()))
          username = item.getString();
        if ("xml".equals(item.getFieldName()))
          wantXml = item.getString().equals("true");
      }
    }

    iter = fileItems.iterator();
    while (iter.hasNext()) {
      FileItem item = (FileItem) iter.next();

      if (!item.isFormField()) {
        try {
          processUploadedFile(req, res, (DiskFileItem) item, username, wantXml );
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

    NetcdfDataset ncd = NetcdfDataset.openDataset(uploadedFile.getPath());
    ncd.setLocation( filename);
    int len = showValidatorResults( res, ncd, wantXml);

    if (req.getRemoteUser() == null) {
      if (username != null)
        MDC.put( "userid", username);
    }

    log.info( "Uploaded File = " + item.getName() + " sent to " + uploadedFile.getPath() + " size= " + uploadedFile.length());
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, len);
  }

  static public int showValidatorResults(HttpServletResponse res, NetcdfDataset ncd, boolean wantXml) throws Exception {

    NetcdfDatasetInfo info = ncd.getInfo();
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
  }

  static private InputStream getXSLT() {
    Class c = CoordSysValidatorServlet.class;
    return c.getResourceAsStream("/resources/thredds/xsl/cdmValidation.xsl"); 
  }
}

