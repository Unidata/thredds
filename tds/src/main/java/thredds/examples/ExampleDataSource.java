// $Id: $
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

package thredds.examples;

import ucar.nc2.NetcdfFile;
import ucar.nc2.IOServiceProvider;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

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
public class ExampleDataSource  implements thredds.servlet.DatasetSource {
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
      super( new BarrodaleIOSP(databaseURL), null, location, null);
    }
  }

  public class BarrodaleIOSP implements IOServiceProvider {
    BarrodaleIOSP( String databaseURL) {
    }

    public boolean isValidFile(RandomAccessFile raf) throws IOException {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    public Array readData(Variable v2, List section) throws IOException, InvalidRangeException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Array readNestedData(Variable v2, List section) throws IOException, InvalidRangeException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws IOException {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean syncExtend() throws IOException {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean sync() throws IOException {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSpecial(Object special) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    public String toStringDebug(Object o) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDetailInfo() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }
}
