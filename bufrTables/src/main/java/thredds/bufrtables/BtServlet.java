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
package thredds.bufrtables;



import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ucar.unidata.util.StringUtil;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.nc2.NetcdfFile;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.DataDescriptor;
import ucar.nc2.iosp.bufr.writer.Bufr2Xml;

/**
 * NOT USED - replaced with spring beans
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
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

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

      try {
        processURL(req, res, urlString, null, wantXml);
        return;
      } catch (Exception e) {
        log.info("Validator processURL", e);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      }

      return;
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
    Message m = null;
    try {
      m = getBufrMessage(file, messno);
      if (m == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messno + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      Formatter f = new Formatter(out);
      f.format("File %s message %d %n%n", cacheName, messno);
      if (!m.isTablesComplete()) {
        f.format(" MISSING DATA DESCRIPTORS= ");
        m.showMissingFields(f);
        f.format("%n%n");
      }
      m.dump(f);
      f.flush();

    } finally {
      if (m != null) {
        m.close();
      }
    }
  }

  private void showMessSize(HttpServletResponse res, File file, String cacheName, int messno) throws IOException {
    Message m = null;
    try {
      m = getBufrMessage(file, messno);
      if (m == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messno + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      Formatter f = new Formatter(out);
      f.format("File %s message %d %n%n", cacheName, messno);

      try {
        int nbitsCounted = m.calcTotalBits(f);
        int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);
        boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.getDataLength()) <= 1; // radiosondes dataLen not even number

        if (!ok) f.format("*** BAD BIT COUNT %n");
        long last = m.dataSection.getDataPos() + m.dataSection.getDataLength();
        DataDescriptor root = m.getRootDataDescriptor();
        f.format("Message nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
            m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
            nbitsCounted, nbitsGiven);
        f.format(" countBits= %d givenBits=%d %n", nbitsCounted, nbitsGiven);
        f.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.getDataLength());
        f.format("%n");

      } catch (Exception ex) {
        ex.printStackTrace();
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
      }
      f.flush();

    } finally {
      if (m != null) {
        m.close();
      }
    }
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
          byte[] mbytes = scan.getMessageBytesFromLast(message);
          NetcdfFile ncfile = null;
          try {
            ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
          } catch (Exception e) {
            throw new IOException(e);
          }
          ncd = new NetcdfDataset(ncfile);
          break;
        }
        count++;
      }

      if (ncd == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messno + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      new Bufr2Xml(message, ncd, out, false);
      out.flush();

    } finally {
      if (ncd != null) ncd.close();
      else if (raf != null) raf.close();
    }
  }

  private Message getBufrMessage(File file, int messno) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      if (count == messno) return m;
      count++;
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
          byte[] mbytes = scan.getMessageBytesFromLast(m);
          NetcdfFile ncfile = null;
          try {
            ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
          } catch (Exception e) {
            throw new IOException(e);
          }
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
      //XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      //FileOutputStream fout = new FileOutputStream("C:/temp/bufr.xml");
      //System.out.print(fmt.outputString(doc));
      //fmt.output(doc, fout);

      showValidatorResults(res, doc, wantXml);


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

  private void processURL(HttpServletRequest req, HttpServletResponse res, String urls, String username, boolean wantXml) throws Exception {

    if ((username == null) || (username.length() == 0)) username = "anon";
    username = StringUtil.filter(username, "_");
    String filename = urls;
    filename = StringUtil.replace(filename, "/", "-");
    filename = StringUtil.filter(filename, ".-_");

    String cacheName = username + "/" + filename;
    File uploadedFile = new File(cacheDir + "/" + cacheName);
    uploadedFile.getParentFile().mkdirs();
    IO.readURLtoFile(urls, uploadedFile);

    try {
      Document doc = readBufr(uploadedFile, cacheName);

      // debug
      //XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      //FileOutputStream fout = new FileOutputStream("C:/temp/bufr.xml");
      //System.out.print(fmt.outputString(doc));
      //fmt.output(doc, fout);

      showValidatorResults(res, doc, wantXml);


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

    log.info("Uploaded File = " + urls + " sent to " + uploadedFile.getPath() + " size= " + uploadedFile.length());
  }

  private Document readBufr(File file, String cacheName) throws IOException {
    long start = System.nanoTime();
    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
    DateFormatter format = new DateFormatter();

    Element rootElem = new Element("bufrValidation");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("fileName", cacheName);
    rootElem.setAttribute("fileSize", Long.toString(raf.length()));
    GregorianCalendar cal = new GregorianCalendar();

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;

      Element bufrMessage = new Element("bufrMessage").setAttribute("status", "ok");
      rootElem.addContent(bufrMessage);

      bufrMessage.setAttribute("pos", Integer.toString(count));
      if (!m.isTablesComplete())
        bufrMessage.setAttribute("dds", "incomplete");
      else
        bufrMessage.setAttribute("dds", "ok");

      int nbitsCounted = m.getTotalBits();
      int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);

      boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.getDataLength()) <= 1; // radiosondes dataLen not even number of bytes
      if (ok)
        bufrMessage.setAttribute("size", "ok");
      else {
        bufrMessage.setAttribute("size", "fail");
        bufrMessage.addContent(
            new Element("ByteCount").setText("countBytes " + m.getCountedDataBytes() + " != " + m.dataSection.getDataLength() + " dataSize"));
      }

      bufrMessage.addContent(new Element("BitCount").setText("countBits " + nbitsCounted + " != " + nbitsGiven + " dataSizeBits"));

      bufrMessage.setAttribute("nobs", Integer.toString(m.getNumberDatasets()));
      bufrMessage.addContent(new Element("WMOheader").setText(extractWMO(m.getHeader())));
      bufrMessage.addContent(new Element("center").setText(m.getCenterName()));
      bufrMessage.addContent(new Element("category").setText(m.getCategoryFullName()));
      bufrMessage.addContent(new Element("date").setText( format.toDateTimeString(m.ids.getReferenceTime(cal))));
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


