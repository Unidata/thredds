/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.dt.grid;

import org.junit.experimental.categories.Category;
import ucar.nc2.dt.GridCoordSystem;
import ucar.ma2.Array;
import junit.framework.TestCase;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since Oct 16, 2009
 */
@Category(NeedsCdmUnitTest.class)
public class TestStag3D extends TestCase {
  public void testSubset() throws Exception {
    ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "ft/grid/stag/bora_feb.nc");

    GeoGrid grid = dataset.findGridByName("u");
    assert null != grid;
    System.out.printf("u shape= %s%n", showShape(grid.getShape()));
    
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    ucar.unidata.geoloc.vertical.VerticalTransform vt = gcs.getVerticalTransform();
    Array a = vt.getCoordinateArray(0);
    System.out.printf("vt shape= %s%n", showShape(a.getShape()));

    dataset.close();
  }

  private String showShape( int[] shape) {
    Formatter f = new Formatter();
    for (int s : shape)
      f.format(" %d", s);
    return f.toString();
  }
}
