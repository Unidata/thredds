/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * From margolis@rap.ucar.edu 3/29/2010
 *
 * @author caron
 * @since Apr 12, 2010
 */
@Category(NeedsCdmUnitTest.class)
public class TestLongitudeWrap {

  @Test
  public void testTimeAxisEval() throws IOException {
    /**
     * The following tests BugFixes.evalTimeAxes, called by ucar.nc2.dt.grid.GridCoordSys.isGridCoordSys
     */
    String testFileFullPath = TestDir.cdmUnitTestDir + "ft/grid/echoTops_runtime.nc";
    GridDataset runtimeDataset = new GridDataset(new NetcdfDataset(NetcdfFile.open(testFileFullPath)));
    if (runtimeDataset.getGrids().isEmpty()) {
      throw new RuntimeException("Runtime data file did not generate a dataset with grids");
    }
    if (runtimeDataset.getGrids().get(0).getCoordinateSystem().getRunTimeAxis() == null) {
      throw new RuntimeException("Runtime data file did not generate a dataset with a RunTime axis");
    }

    System.out.println("BugFixesTest - completed.");
  }

  @Test
  public void testLongitudeWrap() throws Exception {
    /*
     * The following tests BugFixes.findIndexOfLon, called by ucar.nc2.dataset.CoordinateAxis1D.findCoordElementIrregular
     */
    // this file has longitude wrapping
    /* lon =
  {258.0, 259.0, 260.0, 261.0, 262.0, 263.0, 264.0, 265.0, 266.0, 267.0, 268.0, 269.0, 270.0, 271.0, 272.0, 273.0, 274.0, 275.0, 276.0,
  277.0, 278.0, 279.0, 280.0, 281.0, 282.0, 283.0, 284.0, 285.0, 286.0, 287.0, 288.0, 289.0, 290.0, 291.0, 292.0, 293.0, 294.0, 295.0,
  296.0, 297.0, 298.0, 299.0, 300.0, 301.0, 302.0, 303.0, 304.0, 305.0, 306.0, 307.0, 308.0, 309.0, 310.0, 311.0, 312.0, 313.0, 314.0,
  315.0, 316.0, 317.0, 318.0, 319.0, 320.0, 321.0, 322.0, 323.0, 324.0, 325.0, 326.0, 327.0, 328.0, 329.0, 330.0, 331.0, 332.0, 333.0,
  334.0, 335.0, 336.0, 337.0, 338.0, 339.0, 340.0, 341.0, 342.0, 343.0, 344.0, 345.0, 346.0, 347.0, 348.0, 349.0, 350.0, 351.0, 352.0,
  353.0, 354.0, 355.0, 356.0, 357.0, 358.0, 359.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0,
  16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 36.0, 37.0,
  38.0, 39.0, 40.0, 41.0, 42.0, 43.0, 44.0, 45.0, 46.0, 47.0, 48.0, 49.0, 50.0, 51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0,
  60.0, 61.0, 62.0, 63.0, 64.0, 65.0, 66.0, 67.0, 68.0, 69.0, 70.0, 71.0, 72.0, 73.0, 74.0, 75.0, 76.0, 77.0, 78.0, 79.0, 80.0, 81.0,
  82.0, 83.0, 84.0, 85.0, 86.0, 87.0, 88.0, 89.0, 90.0, 91.0, 92.0, 93.0, 94.0, 95.0, 96.0, 97.0, 98.0, 99.0, 100.0, 101.0, 102.0, 103.0,
  104.0, 105.0, 106.0, 107.0, 108.0, 109.0, 110.0, 111.0, 112.0, 113.0, 114.0, 115.0, 116.0, 117.0, 118.0, 119.0, 120.0, 121.0, 122.0,
  123.0, 124.0, 125.0, 126.0, 127.0, 128.0, 129.0, 130.0, 131.0, 132.0, 133.0, 134.0, 135.0, 136.0, 137.0, 138.0, 139.0, 140.0, 141.0,
  142.0, 143.0, 144.0, 145.0, 146.0, 147.0, 148.0, 149.0, 150.0, 151.0, 152.0, 153.0, 154.0, 155.0, 156.0, 157.0, 158.0, 159.0, 160.0,
  161.0, 162.0, 163.0, 164.0, 165.0, 166.0, 167.0, 168.0, 169.0, 170.0, 171.0, 172.0, 173.0, 174.0, 175.0, 176.0, 177.0, 178.0, 179.0,
  180.0, 181.0, 182.0, 183.0, 184.0, 185.0, 186.0, 187.0, 188.0, 189.0, 190.0, 191.0, 192.0, 193.0, 194.0, 195.0, 196.0, 197.0, 198.0,
  199.0, 200.0, 201.0, 202.0, 203.0, 204.0, 205.0, 206.0, 207.0, 208.0, 209.0, 210.0, 211.0, 212.0, 213.0, 214.0, 215.0, 216.0, 217.0,
  218.0, 219.0, 220.0, 221.0, 222.0, 223.0, 224.0, 225.0, 226.0, 227.0, 228.0, 229.0, 230.0, 231.0, 232.0, 233.0, 234.0, 235.0, 236.0,
  237.0, 238.0, 239.0, 240.0, 241.0, 242.0, 243.0, 244.0, 245.0, 246.0, 247.0, 248.0, 249.0, 250.0, 251.0, 252.0, 253.0, 254.0, 255.0, 256.0}
  */

    String testFileFullPath = TestDir.cdmUnitTestDir + "ft/grid/gfs_crossPM_contiguous.nc";
    String targetGridName = "Temperature_altitude_above_msl";

    ////// Test non-Runtime dataset
    GridDataset dataset = GridDataset.open(testFileFullPath);
    GridDatatype targetGrid = dataset.findGridDatatype(targetGridName);
    if (targetGrid == null) {
      throw new RuntimeException("Grid '" + targetGridName + "' does not exist in data file");
    }

    GridCoordSystem coordSys = targetGrid.getCoordinateSystem();
    validateIndices(coordSys, 38.0, -10.0, new int[]{92, 1});
    validateIndices(coordSys, 38.0, 350.0, new int[]{92, 1});
    validateIndices(coordSys, 38.0, -8.0, new int[]{94, 1});
    validateIndices(coordSys, 38.0, 352.0, new int[]{94, 1});
    validateIndices(coordSys, 38.0, -0.6, new int[]{101, 1});
    validateIndices(coordSys, 38.0, 359.4, new int[]{101, 1});
    validateIndices(coordSys, 38.0, 0.0, new int[]{102, 1});
    validateIndices(coordSys, 38.0, 360.0, new int[]{102, 1});
    validateIndices(coordSys, 38.0, 4.0, new int[]{106, 1});
    validateIndices(coordSys, 38.0, 364.0, new int[]{106, 1});
    validateIndices(coordSys, 39.0, -10.0, new int[]{92, 0});
    validateIndices(coordSys, 39.0, 350.0, new int[]{92, 0});
    validateIndices(coordSys, 39.0, -8.0, new int[]{94, 0});
    validateIndices(coordSys, 39.0, 352.0, new int[]{94, 0});
    validateIndices(coordSys, 39.0, -0.6, new int[]{101, 0});
    validateIndices(coordSys, 39.0, 359.4, new int[]{101, 0});
    validateIndices(coordSys, 39.0, 0.0, new int[]{102, 0});
    validateIndices(coordSys, 39.0, 360.0, new int[]{102, 0});
    validateIndices(coordSys, 39.0, 4.0, new int[]{106, 0});
    validateIndices(coordSys, 39.0, 364.0, new int[]{106, 0});

    validateIndicesBounded(coordSys, 38.0, -20.0, new int[]{82, 1});
    validateIndicesBounded(coordSys, 38.0, 340.0, new int[]{82, 1});
    validateIndicesBounded(coordSys, 38.0, 10.0, new int[]{112, 1});
    validateIndicesBounded(coordSys, 38.0, 370.0, new int[]{112, 1});
    validateIndicesBounded(coordSys, 39.0, -20.0, new int[]{82, 0});
    validateIndicesBounded(coordSys, 39.0, 340.0, new int[]{82, 0});
    validateIndicesBounded(coordSys, 39.0, 10.0, new int[]{112, 0});
    validateIndicesBounded(coordSys, 39.0, 370.0, new int[]{112, 0});
  }

  private static void validateIndices(GridCoordSystem coordSystem, double lat, double lon, int[] expectedIndices)
          throws Exception {
    validateIndices(coordSystem, lat, lon, expectedIndices, false);
  }

  private static void validateIndicesBounded(GridCoordSystem coordSystem, double lat, double lon, int[] expectedIndices)
          throws Exception {
    validateIndices(coordSystem, lat, lon, expectedIndices, true);
  }

  private static void validateIndices(GridCoordSystem coordSystem, double lat, double lon, int[] expectedIndices, boolean bounded)
          throws Exception {
    int[] indices = (bounded) ?
            coordSystem.findXYindexFromLatLonBounded(lat, lon, null) :
            coordSystem.findXYindexFromLatLon(lat, lon, null);

    if (indices[0] != expectedIndices[0]) {
      throw new Exception("(latitude " + lat + ", longitude " + lon + ") expected index[0]==" + expectedIndices[0] + "; received index[0]==" + indices[0]);
    }
    if (indices[1] != expectedIndices[1]) {
      throw new Exception("(latitude " + lat + ", longitude " + lon + ") expected index[1]==" + expectedIndices[1] + "; received index[1]==" + indices[1]);
    }
  }

}
