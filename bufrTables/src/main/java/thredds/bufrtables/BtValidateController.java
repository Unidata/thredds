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

import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.HashSet;
import java.nio.channels.WritableByteChannel;

/**
 * @author caron
 * @since Oct 3, 2008
 */
public class BtValidateController extends AbstractCommandController {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BtValidateController.class);

  private DiskCache2 diskCache = null;
  private Set<Message> messSet = new HashSet<Message>();
  private WritableByteChannel wbc = null;
  private FileOutputStream fos;

  public void setCache(DiskCache2 cache) {
    diskCache = cache;
  }

  public void setUniqueMessageFile(String filename) {
    File f = new File(filename);
    if (f.exists()) {

      // read in the file
      RandomAccessFile raf = null;
      try {
        raf = new RandomAccessFile(filename, "r");

        MessageScanner scan = new MessageScanner(raf);
        while (scan.hasNext()) {
          Message m = scan.next();
          if (m == null) {
            log.warn("Bad message in file "+filename);
            continue;
          }

          if (!messSet.contains(m)) {
            messSet.add(m);
          }
        }

      } catch (IOException e) {
        log.error("Failed to read in unique message file "+filename, e);

      } finally {
        if (raf != null)
          try { raf.close(); }
          catch (IOException e) { }
      }
    }

    // reopen for writing
    try {
      fos = new FileOutputStream(filename, true);
      wbc = fos.getChannel();
    } catch (FileNotFoundException e) {
      log.error("Failed to open for writing unique message file "+filename, e);
    }

  }

  private synchronized void addUniqueMessages(String filename) {
    if (wbc == null) return;

    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(filename, "r");

      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) {
          log.warn("Bad message in file "+filename);
          continue;
        }

        if (!messSet.contains(m)) {
          scan.writeCurrentMessage(wbc);
          messSet.add(m);
        }
      }
      fos.flush();

    } catch (IOException e) {
      log.error("Failed to read message file "+filename, e);

    } finally {
      if (raf != null)
        try { raf.close(); }
        catch (IOException e) { }
    }

  }

  protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
    // cast the bean
    FileValidateBean bean = (FileValidateBean) command;

    String fname = StringUtil.unescape(bean.getFilename());
    File dest = diskCache.getCacheFile(fname);
    addUniqueMessages(dest.getPath());

    try {
      Document doc = makeXml(dest, fname);
      if (bean.isXml()) {
        return new ModelAndView("xmlView", "doc", doc);
      } else {
        return new ModelAndView("xsltView", "source", new JDOMSource(doc));
      }
    } catch (Exception e) {
      logger.warn("Exception on file " + fname, e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File=" + fname);
      return null;
    }
  }

  protected Document makeXml(File f, String filename) throws Exception {
    DateFormatter format = new DateFormatter();
    Element rootElem = new Element("bufrValidation");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("fileName", filename);
    rootElem.setAttribute("fileSize", Long.toString(f.length()));

    GregorianCalendar cal = new GregorianCalendar();
    RandomAccessFile raf = new RandomAccessFile(f.getPath(), "r");
    try {
      int count = 0;
      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;

        Element bufrMessage = new Element("bufrMessage").setAttribute("status", "ok");
        rootElem.addContent(bufrMessage);

        bufrMessage.setAttribute("record", Integer.toString(count));
        bufrMessage.setAttribute("pos", Long.toString(m.getStartPos()));
        if (!m.isTablesComplete())
          bufrMessage.setAttribute("dds", "incomplete");
        else
          bufrMessage.setAttribute("dds", "ok");

        int nbitsCounted = -1;
        try {
          nbitsCounted = m.getTotalBits();
        } catch (Exception e) {
          // ok if bit counting fails
        }

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
        bufrMessage.addContent(new Element("WMOheader").setText(m.extractWMO()));
        bufrMessage.addContent(new Element("center").setText(m.getCenterName()));
        bufrMessage.addContent(new Element("category").setText(m.getCategoryFullName()));
        bufrMessage.addContent(new Element("date").setText(format.toDateTimeString(m.ids.getReferenceTime(cal))));
        count++;
      }
      return doc;
    } finally {
      if (raf != null) raf.close();
    }
  }

}

