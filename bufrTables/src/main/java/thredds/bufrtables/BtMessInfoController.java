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

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.util.DiskCache2;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.bufr.Message;
import ucar.bufr.Dump;
import ucar.bufr.DataDescriptor;
import ucar.bufr.MessageScanner;
import ucar.unidata.io.RandomAccessFile;

import java.util.Formatter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 2, 2008
 */
public class BtMessInfoController extends AbstractController {
  private DiskCache2 diskCache = null;

  public void setCache(DiskCache2 cache) {
    diskCache = cache;
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String path = req.getPathInfo();
    System.out.println(" BtController.handleRequestInternal got " + path);

    // info about existing files
    if (path.startsWith("/mess/")) {
      path = path.substring(6);

      // cmd
      int pos = path.lastIndexOf("/");
      if (pos < 0) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "no message number");
        return null;
      }
      String cmd = path.substring(pos + 1);
      path = path.substring(0, pos);

      // messno
      pos = path.lastIndexOf("/");
      if (pos < 0) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "no message number");
        return null;
      }
      String mess = path.substring(pos + 1);
      long messPos;
      try {
        messPos = Long.parseLong(mess);
      } catch (Exception e) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "illegal message number=" + mess);
        return null;
      }

      // cacheFile
      String cacheName = path.substring(0, pos);
      File uploadedFile = diskCache.getCacheFile(cacheName);
      if (!uploadedFile.exists()) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "file not found=" + cacheName + "; may have been purged");
        return null;
      }

      if (cmd.equals("data.xml"))
        showMessData(res, uploadedFile, cacheName, messPos);
      else if (cmd.equals("dds.txt"))
        showMessDDS(res, uploadedFile, cacheName, messPos);
      else if (cmd.equals("bitCount.txt"))
        showMessSize(res, uploadedFile, cacheName, messPos);
      else if (cmd.equals("table.txt"))
        showMessTable(res, uploadedFile, cacheName, messPos);

      return null;
    }

    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "GET request not understood");
    return null;
  }

  private void showMessDDS(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
    Message m = null;
    try {
      m = getBufrMessageByPos(file, messPos);
      if (m == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messPos + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      Formatter f = new Formatter(out);
      f.format("File %s message at pos %d %n%n", cacheName, messPos);
      if (!m.isTablesComplete()) {
        f.format(" MISSING DATA DESCRIPTORS= ");
        m.showMissingFields(f);
        f.format("%n%n");
      }
      new Dump().dump(f, m);
      f.flush();

    } finally {
      if (m != null) {
        m.close();
      }
    }
  }

  private void showMessSize(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
    Message m = null;
    try {
      m = getBufrMessageByPos(file, messPos);
      if (m == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messPos + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      Formatter f = new Formatter(out);
      f.format("File %s message at pos %d %n%n", cacheName, messPos);

      try {
        int nbitsCounted = m.calcTotalBits(f);
        int nbitsGiven = 8 * (m.dataSection.dataLength - 4);
        boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.dataLength) <= 1; // radiosondes dataLen not even number

        if (!ok) f.format("*** BAD BIT COUNT %n");
        DataDescriptor root = m.getRootDataDescriptor();
        f.format("%nMessage nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
            m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
            nbitsCounted, nbitsGiven);
        f.format(" countBits= %d givenBits=%d %n", nbitsCounted, nbitsGiven);
        f.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.dataLength);
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

  private void showMessTable(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
    Message m = null;
    try {
      m = getBufrMessageByPos(file, messPos);
      if (m == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messPos + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      Formatter f = new Formatter(out);
      f.format("File %s message at pos %d %n%n", cacheName, messPos);

      try {
        int nbitsCounted = m.calcTotalBits(f);
        int nbitsGiven = 8 * (m.dataSection.dataLength - 4);
        boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.dataLength) <= 1; // radiosondes dataLen not even number

        if (!ok) f.format("*** BAD BIT COUNT %n");
        DataDescriptor root = m.getRootDataDescriptor();
        f.format("%nMessage nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
            m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
            nbitsCounted, nbitsGiven);
        f.format(" countBits= %d givenBits=%d %n", nbitsCounted, nbitsGiven);
        f.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.dataLength);
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

  private void showMessData(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
    Message message = null;
    NetcdfDataset ncd = null;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(file.getPath(), "r");
      MessageScanner scan = new MessageScanner(raf, messPos);
      if (scan.hasNext()) {
        message = scan.next();
        byte[] mbytes = scan.getMessageBytesFromLast(message);
        NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes);
        ncd = new NetcdfDataset(ncfile);
      }

      if (ncd == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message at pos=" + messPos + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/xml; charset=UTF-8");
      try {
        OutputStream out = res.getOutputStream();
        new Bufr2Xml(message, ncd, out);
        out.flush();
      } catch (Exception e) {
        logger.warn("Exception on file "+cacheName,e);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "message at pos=" + messPos + " cant be read, filename= " + cacheName);
      }

    } finally {
      if (ncd != null) ncd.close();
      if (raf != null) raf.close();
    }
  }

  /* private Message getBufrMessage(File file, int messno) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      if (count == messno) return m;
      count++;
    }

    if (raf != null) raf.close();
    return null;
  } */

  private Message getBufrMessageByPos(File file, long pos) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
    MessageScanner scan = new MessageScanner(raf, pos);
    if (scan.hasNext())
      return scan.next();

    if (raf != null) raf.close();
    return null;
  }


}
