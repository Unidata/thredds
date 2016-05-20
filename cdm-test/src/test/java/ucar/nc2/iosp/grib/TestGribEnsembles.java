/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.collection.GribIosp;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Category(NeedsCdmUnitTest.class)
public class TestGribEnsembles {

  // from jitka
  @Test
  public void testWMOgrib2() throws Exception {

    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib";
    System.out.printf("Open %s%n", filename);
    try (NetcdfFile datafile = NetcdfFile.open(filename)) {
      NetcdfDataset netcdfDataset = new NetcdfDataset(datafile);
      GridDataset gridDataset = new GridDataset(netcdfDataset);

      String variableName = "VAR_10-3-192_L1";

      GridDatatype gridDatatype = gridDataset.findGridDatatypeByAttribute(GribIosp.VARIABLE_ID_ATTNAME, variableName);
      assertNotNull(gridDatatype);

      Dimension rtDimension = gridDatatype.getRunTimeDimension();
      Dimension ensDimension = gridDatatype.getEnsembleDimension();
      Dimension timeDimension = gridDatatype.getTimeDimension();
      Dimension xDimension = gridDatatype.getXDimension();
      Dimension yDimension = gridDatatype.getYDimension();
      Dimension zDimension = gridDatatype.getZDimension();

      assertNull(rtDimension);
      assertNotNull(ensDimension);
      assertNotNull(timeDimension);
      assertNotNull(xDimension);
      assertNotNull(yDimension);
      assertNull(zDimension);

      // test ordering
      int rtIndex = gridDatatype.getRunTimeDimensionIndex();
      int ensIndex = gridDatatype.getEnsembleDimensionIndex();
      int timeIndex = gridDatatype.getTimeDimensionIndex();
      int yIndex = gridDatatype.getYDimensionIndex();
      int xIndex = gridDatatype.getXDimensionIndex();
      int zIndex = gridDatatype.getZDimensionIndex();

      assertEquals(-1, rtIndex);
      assertEquals(0, ensIndex);
      assertEquals(1, timeIndex);
      assertEquals(2, yIndex);
      assertEquals(3, xIndex);
      assertEquals(-1, zIndex);

      Dimension ensDim = gridDatatype.getDimension(ensIndex); //ensIndex = 0
      assertEquals(1, ensDim.getLength());
      assertEquals("ens", ensDim.getShortName());

      Variable variable = gridDatatype.getVariable().getOriginalVariable();
      ensDim = variable.getDimension(ensIndex); //ensIndex = 0
    }

  }

  @Test
  public void testEcmwfEns() throws Exception {

    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB";
    System.out.printf("Open %s%n", filename);

    try (NetcdfFile datafile = NetcdfFile.open(filename)) {
      NetcdfDataset netcdfDataset = new NetcdfDataset(datafile);
      GridDataset gridDataset = new GridDataset(netcdfDataset);

      String requiredName = "Total_precipitation_surface";
      GridDatatype gridDatatype = gridDataset.findGridDatatype(requiredName);
      assertNotNull(gridDatatype);
      assertEquals(requiredName, gridDatatype.getFullName());

      Dimension ensDimension = gridDatatype.getEnsembleDimension();
      assertNotNull(ensDimension); //fails in 4.3 , null returned
      assertEquals(51, ensDimension.getLength()); // is 2 in 4.2, however it should be 51 (incl. control forecast)

      Dimension timeDimension = gridDatatype.getTimeDimension();
      assertEquals(1, timeDimension.getLength()); //ok in both versions

      Dimension xDimension = gridDatatype.getXDimension();
      assertEquals(31, xDimension.getLength()); //ok in both versions

      Dimension yDimension = gridDatatype.getYDimension();
      assertEquals(21, yDimension.getLength()); //ok in both versions
    }
  }

}
