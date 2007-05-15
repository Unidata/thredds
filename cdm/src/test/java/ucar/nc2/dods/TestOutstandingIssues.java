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

package ucar.nc2.dods;


import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

import java.io.*;

/** Test nc2 dods in the JUnit framework. */
public class TestOutstandingIssues extends TestCase {

  public void testByteAttribute() throws IOException {
    String filename = "dods://localhost:8080/thredds/dodsC/CdataScan/profiler/PROFILER_wind_06min_20070514_2354.nc";
    NetcdfDataset ncd = NetcdfDataset.openDataset(filename, true, null);
    assert ncd != null;
    VariableDS v = (VariableDS) ncd.findVariable("uvQualityCode");
    assert v != null;
    assert v.hasMissing();

    int count = 0;
    Array data = v.read();
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      byte val = ii.getByteNext();
      if (v.isMissing(val)) count++;
      if (val == (byte)-1)
        assert v.isMissing(val);
    }
    System.out.println("size = "+v.getSize()+" missing= "+count);
  }
}
