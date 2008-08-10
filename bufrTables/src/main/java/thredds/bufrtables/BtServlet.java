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
package thredds.bufrtables;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItem;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ucar.unidata.util.StringUtil;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.bufr.MessageScanner;
import ucar.bufr.Message;
import ucar.bufr.Dump;

/**
 * @author caron
 * @since Aug 9, 2008
 */
public class BtServlet extends HttpServlet {
  protected org.slf4j.Logger log;

  private DiskCache2 cdmValidateCache = null;
  private DiskFileItemFactory factory;
  private File cacheDir;
  private long maxFileUploadSize = 20 * 1000 * 1000;
  boolean allow = true;
  boolean deleteImmediately = true;

  protected String contentPath;

  public void init() throws javax.servlet.ServletException {
    log = org.slf4j.LoggerFactory.getLogger(getClass());

    String cacheDirName = getInitParameter("CacheDir");
    int scourSecs = Integer.parseInt(getInitParameter("CacheScourSecs"));
    int maxAgeSecs = Integer.parseInt(getInitParameter("CacheMaxAgeSecs"));

    if (maxAgeSecs > 0) {
      deleteImmediately = false;
      cdmValidateCache = new DiskCache2(cacheDirName, false, maxAgeSecs / 60, scourSecs / 60);
    }

    cacheDir = new File(cacheDirName);
    cacheDir.mkdirs();
    factory = new DiskFileItemFactory(0, cacheDir); // LOOK can also do in-memory
  }

  public void destroy() {
    if (cdmValidateCache != null)
      cdmValidateCache.exit();
    super.destroy();
  }

  /**
   * GET handles the case where its a remote URL (dods or http)
   *
   * @param req request
   * @param res response
   * @throws ServletException
   * @throws java.io.IOException
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    String urlString = req.getParameter("URL");
    if (urlString != null) {
      // validate the uri String
      try {
        URI uri = new URI(urlString);
        urlString = uri.toASCIIString(); // LOOK do we want just toString() ? Is this useful "input validation" ?
      } catch (URISyntaxException e) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "URISyntaxException on URU parameter");
        return;
      }

      String xml = req.getParameter("xml");
      boolean wantXml = (xml != null) && xml.equals("true");
    }

    // info about existing files
    String path = req.getPathInfo();
    if (path.startsWith("/mess/")) {
      path = path.substring(6);

      // cmd
      int pos = path.lastIndexOf("/");
      if (pos < 0) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "no message number");
        return;
      }
      String cmd = path.substring(pos + 1);
      path = path.substring(0, pos);

      // messno
      pos = path.lastIndexOf("/");
      if (pos < 0) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "no message number");
        return;
      }
      String mess = path.substring(pos + 1);
      int messno;
      try {
        messno = Integer.parseInt(mess);
      } catch (Exception e) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "illegal message number=" + mess);
        return;
      }

      // cacheFile
      String cacheName = path.substring(0, pos);
      File uploadedFile = new File(cacheDir + "/" + cacheName);
      if (!uploadedFile.exists()) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "file not found=" + cacheName);
        return;
      }

      if (cmd.equals("dds.txt"))
        showMessDDS(res, uploadedFile, cacheName, messno);
      else if (cmd.equals("data.txt"))
        showMessData(res, uploadedFile, cacheName, messno);
      else if (cmd.equals("bitCount.txt"))
        showMessSize(res, uploadedFile, cacheName, messno);
    }

    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "GET request not understood");
  }

  private void showMessDDS(HttpServletResponse res, File file, String cacheName, int messno) throws IOException {
    Message m = getBufrMessage(file, messno);
    if (m == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messno + " not found in " + cacheName);
      return;
    }

    res.setContentType("text/plain");
    OutputStream out = res.getOutputStream();
    Formatter f = new Formatter(out);
    f.format("File %s message %d %n%n", cacheName, messno);
    new Dump().dump(f, m);
    f.flush();
  }

  private void showMessSize(HttpServletResponse res, File file, String cacheName, int messno) throws IOException {
    Message m = getBufrMessage(file, messno);
    if (m == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messno + " not found in " + cacheName);
      return;
    }

    res.setContentType("text/plain");
    OutputStream out = res.getOutputStream();
    Formatter f = new Formatter(out);
    f.format("File %s message %d %n%n", cacheName, messno);
    new Dump().dump(f, m);
    f.flush();
  }

  private void showMessData(HttpServletResponse res, File file, String cacheName, int messno) throws IOException {
    Message message = null;
    NetcdfDataset ncd = null;

    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(file.getPath(), "r");
      MessageScanner scan = new MessageScanner(raf);
      int count = 0;
      while (scan.hasNext()) {
        message = scan.next();
        if (message == null) continue;
        if (count == messno) {
          byte[] mbytes = scan.getMessageBytes( message);
          NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes);
          ncd = new NetcdfDataset(ncfile);
        }
        count++;
      }
    } finally {
      if (raf != null) raf.close();
    }

    if (ncd == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messno + " not found in " + cacheName);
      return;
    }

    try {
      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      new Bufr2Xml(message, ncd, out);
      out.flush();

    } finally {
      ncd.close();
    }
  }

  private Message getBufrMessage(File file, int messno) throws IOException {
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(file.getPath(), "r");
      MessageScanner scan = new MessageScanner(raf);
      int count = 0;
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        if (count == messno) return m;
        count++;
      }
    } finally {
      if (raf != null) raf.close();
    }

    return null;
  }

  private NetcdfDataset getBufrMessageAsDataset(File file, int messno) throws IOException {
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(file.getPath(), "r");
      MessageScanner scan = new MessageScanner(raf);
      int count = 0;
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        if (count == messno) {
          byte[] mbytes = scan.getMessageBytes(m);
          NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes);
          NetcdfDataset ncd = new NetcdfDataset(ncfile);
          return ncd;
        }
        count++;
      }
    } finally {
      if (raf != null) raf.close();
    }

    return null;
  }

  /**
   * POST handles uploaded files
   *
   * @param req request
   * @param res response
   * @throws ServletException
   * @throws IOException
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    // Check that we have a file upload request
    boolean isMultipart = ServletFileUpload.isMultipartContent(req);
    if (!isMultipart) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "POST must be multipart");
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
      res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
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
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
      }
    }
  }

  private void processUploadedFile(HttpServletRequest req, HttpServletResponse res, DiskFileItem item,
                                   String username, boolean wantXml) throws Exception {

    if ((username == null) || (username.length() == 0)) username = "anon";
    username = StringUtil.filter(username, "_");
    String filename = item.getName();
    filename = StringUtil.replace(filename, "/", "-");
    filename = StringUtil.filter(filename, ".-_");

    String cacheName = username + "/" + filename;
    File uploadedFile = new File(cacheDir + "/" + cacheName);
    uploadedFile.getParentFile().mkdirs();
    item.write(uploadedFile);

    try {
      Document doc = readBufr(uploadedFile, cacheName);

      // debug
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      FileOutputStream fout = new FileOutputStream("C:/temp/bufr.xml");
      System.out.print(fmt.outputString(doc));
      fmt.output(doc, fout);

      int len = showValidatorResults(res, doc, wantXml);


    } finally {

      if (deleteImmediately) {
        try {
          uploadedFile.delete();
        } catch (Exception e) {
          log.error("Uploaded File = " + uploadedFile.getPath() + " delete failed = " + e.getMessage());
        }
      }
    }


    if (req.getRemoteUser() == null) {
    }

    log.info("Uploaded File = " + item.getName() + " sent to " + uploadedFile.getPath() + " size= " + uploadedFile.length());
  }

  private Document readBufr(File file, String cacheName) throws Exception {
    long start = System.nanoTime();
    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");

    Element rootElem = new Element("bufrValidation");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("fileName", cacheName);
    rootElem.setAttribute("fileSize", Long.toString(raf.length()));

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;

      Element bufrMessage = new Element("bufrMessage").setAttribute("status", "ok");
      rootElem.addContent(bufrMessage);

      bufrMessage.setAttribute("pos", Integer.toString(count));
      if (!m.hasTablesComplete())
        bufrMessage.setAttribute("dds", "incomplete");
      else
        bufrMessage.setAttribute("dds", "ok");

      int nbitsCounted = m.getTotalBits();
      int nbitsGiven = 8 * (m.dataSection.dataLength - 4);

      boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.dataLength) <= 1; // radiosondes dataLen not even number of bytes
      if (ok)
        bufrMessage.setAttribute("size", "ok");
      else {
        bufrMessage.setAttribute("size", "fail");
        bufrMessage.addContent(
            new Element("ByteCount").setText("countBytes " + m.getCountedDataBytes() + " != " + m.dataSection.dataLength + " dataSize"));
      }

      bufrMessage.addContent(new Element("BitCount").setText("countBits " + nbitsCounted + " != " + nbitsGiven + " dataSizeBits"));

      bufrMessage.setAttribute("nobs", Integer.toString(m.getNumberDatasets()));
      bufrMessage.addContent(new Element("WMOheader").setText(extractWMO(m.header)));
      bufrMessage.addContent(new Element("center").setText(m.getCenterName()));
      bufrMessage.addContent(new Element("category").setText(m.getCategoryFullName()));
      bufrMessage.addContent(new Element("date").setText(m.ids.getReferenceTime()));
      count++;
    }
    raf.close();

    rootElem.setAttribute("totalObs", Integer.toString(scan.getTotalObs()));
    return doc;
  }

  private static final Pattern wmoPattern = Pattern.compile(".*([IJ]..... ....) .*");

  private String extractWMO(String header) {
    Matcher matcher = wmoPattern.matcher(header);
    if (!matcher.matches()) {
      log.warn("extractWMO failed= %s\n", header);
      return header;
    }
    return matcher.group(1);
  }

  private int showValidatorResults(HttpServletResponse res, Document doc, boolean wantXml) throws Exception {
    String infoString;

    if (wantXml) {
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);
      res.setContentLength(infoString.length());
      res.setContentType("text/xml; charset=iso-8859-1");

    } else {
      InputStream is = getXSLT();
      XSLTransformer transformer = new XSLTransformer(is);

      Document html = transformer.transform(doc);
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(html);

      res.setContentType("text/html; charset=iso-8859-1");
    }

    res.setContentLength(infoString.length());

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    return infoString.length();
  }

  private InputStream getXSLT() {
    Class c = this.getClass();
    String resource = "/resources/xsl/validation.xsl";
    InputStream is = c.getResourceAsStream(resource);
    if (null == is)
      log.error("Cant load XSLT resource = " + resource);

    return is;
  }
}


