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

import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.BindException;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import ucar.nc2.util.DiskCache2;
import ucar.bufr.Message;
import ucar.bufr.MessageScanner;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

/**
 * @author caron
 * @since Oct 3, 2008
 */
public class BtValidateController extends AbstractCommandController {
  private DiskCache2 diskCache = null;

  public void setCache(DiskCache2 cache) {
    diskCache = cache;
  }

  protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
    // cast the bean
    FileValidateBean bean = (FileValidateBean) command;

    String fname = StringUtil.unescape(bean.getFilename());
    File dest = diskCache.getCacheFile(fname);

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

    Element rootElem = new Element("bufrValidation");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("fileName", filename);
    rootElem.setAttribute("fileSize", Long.toString(f.length()));

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
        bufrMessage.addContent(new Element("WMOheader").setText(m.extractWMO()));
        bufrMessage.addContent(new Element("center").setText(m.getCenterName()));
        bufrMessage.addContent(new Element("category").setText(m.getCategoryFullName()));
        bufrMessage.addContent(new Element("date").setText(m.ids.getReferenceTime()));
        count++;
      }
      return doc;
    } finally {
      if (raf != null) raf.close();
    }
  }

}

