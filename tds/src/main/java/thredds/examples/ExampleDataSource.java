// $Id: $
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

package thredds.examples;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class ExampleDataSource implements thredds.servlet.DatasetSource {
  HashMap hash = new HashMap();

  public boolean isMine(HttpServletRequest req) {
    String path = req.getPathInfo();
    return path.startsWith("/barrodale/");
  }

  public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String path = req.getPathInfo();
    path = path.substring("/barrodale/".length());

    String databaseURL = (String) hash.get(path);
    if (databaseURL == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return null;
    }

    return new BarrodaleNetcdfFile(req.getRequestURI(), databaseURL);
  }

  public class BarrodaleNetcdfFile extends NetcdfFile {
    BarrodaleNetcdfFile(String location, String databaseURL) throws IOException {
      super(new BarrodaleIOSP(databaseURL), null, location, null);
    }
  }

  public class BarrodaleIOSP extends AbstractIOServiceProvider {
    BarrodaleIOSP(String databaseURL) {
    }

    public boolean isValidFile(RandomAccessFile raf) throws IOException {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getFileTypeId() {
      return "Barrodale";
    }

    public String getFileTypeDescription() {
      return "Barrodale";
    }

    public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws IOException {
      //To change body of implemented methods use File | Settings | File Templates.
    }

  }
}
