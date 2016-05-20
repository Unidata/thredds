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
package ucar.nc2.dt.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

@Category(NeedsExternalResource.class)
public class Test3dFromOpendap {
  @Test
  public void test3D() throws Exception {
    try (GridDataset dataset = GridDataset.open("dods://"+ TestDir.threddsTestServer+"/thredds/dodsC/grib/NCEP/NAM/CONUS_12km/best")) {

      GeoGrid grid = dataset.findGridByName("Relative_humidity_isobaric");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      GeoGrid grid_section = grid.subset(null, null, null, 1, 10, 10);

      Array data = grid_section.readDataSlice(0, -1, -1, -1);
      assert data.getRank() == 3;
      // assert data.getShape()[0] == 6 : data.getShape()[0];
      assert data.getShape()[1] == 43 : data.getShape()[1];
      assert data.getShape()[2] == 62 : data.getShape()[2];

      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        float val = ii.getFloatNext();
        if (grid_section.isMissingData(val)) {
          if (!Float.isNaN(val)) {
            System.out.println(" got not NaN at =" + ii);
          }
          int[] current = ii.getCurrentCounter();
          if ((current[1] > 0) && (current[2] > 1)) {
            System.out.println(" got missing at =" + ii);
            System.out.println(current[1] + " " + current[2]);
          }
        }
      }

    }
  }

}

