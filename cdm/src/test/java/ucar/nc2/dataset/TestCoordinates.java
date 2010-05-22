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

package ucar.nc2.dataset;


import junit.framework.*;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.NCdumpW;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;

import java.io.IOException;

/** Test _Coordinates dataset in the JUnit framework. */

public class TestCoordinates extends TestCase {

  public TestCoordinates( String name) {
    super(name);
  }

  public void testAlias() throws IOException {
    String filename = TestAll.cdmUnitTestDir + "fmrc/ensemble/demeter/MM_cnrm_129_red.ncml";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset( filename);
    Variable v = ncd.findCoordinateAxis("number");
    assert v != null;
    //assert v.isCoordinateVariable();
    assert v instanceof CoordinateAxis1D;
    assert null != ncd.findDimension("ensemble");
    assert v.getDimension(0) == ncd.findDimension("ensemble");
  }


  // test offset only gets applied once
  public void testWrapOnce() throws IOException {
    String filename = TestAll.cdmUnitTestDir + "ncml/coords/testCoordScaling.ncml";
    System.out.printf("%s%n", filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset( filename);
    Variable v = ncd.findCoordinateAxis("Longitude");
    assert v != null;
    assert v instanceof CoordinateAxis1D;

    // if offset is applied twice, the result is not in +-180 range
    Array data = v.read();
    NCdumpW.printArray(data);
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      assert Math.abs(ii.getDoubleNext()) < 180 : ii.getDoubleCurrent();
    }
  }

}
