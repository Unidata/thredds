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

package ucar.nc2.dataset;


import junit.framework.*;
import ucar.nc2.TestAll;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;

/** Test _Coordinates dataset in the JUnit framework. */

public class TestCoordinates extends TestCase {

  public TestCoordinates( String name) {
    super(name);
  }

  public void testAlias() throws IOException {
    String filename = TestAll.upcShareTestDataDir + "fmrc/MM_cnrm_129_red.ncml";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset( filename);
    Variable v = ncd.findCoordinateAxis("number");
    assert v != null;
    //assert v.isCoordinateVariable();
    assert v instanceof CoordinateAxis1D;
    assert null != ncd.findDimension("ensemble");
    assert v.getDimension(0) == ncd.findDimension("ensemble");
  }
}
