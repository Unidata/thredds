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

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.util.DiskCache2;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.iosp.bufr.writer.Bufr2Xml;
import ucar.unidata.io.RandomAccessFile;

import java.util.Formatter;
import java.io.*;

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
      m.dump(f);
      f.flush();

    } finally {
      if (m != null) {
        m.close();
      }
    }
  }

  private void showMessSizeOld(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
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
        int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);
        boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.getDataLength()) <= 1; // radiosondes dataLen not even number

        if (!ok) f.format("*** BAD BIT COUNT %n");
        DataDescriptor root = m.getRootDataDescriptor();
        f.format("%nMessage nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
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

  private void showMessSize(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
    res.setContentType("text/plain");

    RandomAccessFile raf = null;
    Message m = null;
    try {
      raf = new RandomAccessFile(file.getPath(), "r");
      MessageScanner scan = new MessageScanner(raf, messPos);
      if (scan.hasNext())
        m = scan.next();

      if (m == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message " + messPos + " not found in " + cacheName);
        return;
      }

      res.setContentType("text/plain");
      OutputStream out = res.getOutputStream();
      Formatter f = new Formatter(out);
      f.format("File %s message at pos %d %n%n", cacheName, messPos);
      if (!m.dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        reader.readData(null, m, raf, null, false, f);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        reader.readData(null, m, raf, null, f);
      }

      int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);
      DataDescriptor root = m.getRootDataDescriptor();
      f.format("Message nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
          m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
          m.getCountedDataBits(), nbitsGiven);
      f.format(" countBits= %d givenBits=%d %n", m.getCountedDataBits(), nbitsGiven);
      f.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.getDataLength());
      f.format("%n");
      f.flush();

    } catch (Exception ex) {
      ex.printStackTrace();
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());

    } finally {
      if (m != null) {
        m.close();
      }

      if (raf != null) raf.close();
    }
  }

  private void showMessTable(HttpServletResponse res, File file, String cacheName, long messPos) throws IOException {
    res.setContentType("text/plain");
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
        int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);
        boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.getDataLength()) <= 1; // radiosondes dataLen not even number

        if (!ok) f.format("*** BAD BIT COUNT %n");
        DataDescriptor root = m.getRootDataDescriptor();
        f.format("%nMessage nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
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
        NetcdfFile ncfile = null;
        try {
          ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
        } catch (Exception e) {
          throw new IOException(e);
        }
        ncd = new NetcdfDataset(ncfile);
      }

      if (ncd == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "message at pos=" + messPos + " not found in " + cacheName);
        return;
      }

      res.setContentType("application/xml; charset=UTF-8");
      try {
        OutputStream out = res.getOutputStream();
        new Bufr2Xml(message, ncd, out, false);
        out.flush();
      } catch (Exception e) {
        logger.warn("Exception on file " + cacheName, e);
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
