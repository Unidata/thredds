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

import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Test basic projection methods
 */
@Category(NeedsCdmUnitTest.class)
public class TestVertical extends TestCase {

  public TestVertical(String name) {
    super(name);
  }

  public void testOceanS() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(TestDir.cdmUnitTestDir + "transforms/roms_ocean_s_coordinate.nc");

    GridDatatype grid = gds.findGridDatatype("temp");
    assert grid != null;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.OceanS;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testOceanSigma() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( TestDir.cdmUnitTestDir + "conventions/cf/gomoos_cf.nc");

    GridDatatype grid = gds.findGridDatatype("temp");
    assert grid != null;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.OceanSigma;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    CoordinateAxis1DTime taxis = gcs.getTimeAxis1D();
    for (int t=0; t<taxis.getSize(); t++) {
      System.out.printf("vert coord for time = %s%n", taxis.getTimeDate(t));
      ArrayDouble.D3 ca = vt.getCoordinateArray(t);
      assert ca != null;
      assert ca.getRank() == 3 : ca.getRank();

      int[] shape = ca.getShape();
      for (int i = 0; i < 3; i++)
        System.out.println(" shape " + i + " = " + shape[i]);
    }
    gds.close();
  }

  public void testAtmSigma() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( TestDir.cdmUnitTestDir + "transforms/temperature.nc");

    GridDatatype grid = gds.findGridDatatype("Temperature");
    assert grid != null;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.Sigma;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testAtmHybrid() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( TestDir.cdmUnitTestDir + "conventions/cf/ccsm2.nc");

    GridDatatype grid = gds.findGridDatatype("T");
    assert grid != null;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.HybridSigmaPressure : vct.getVerticalTransformType();

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testWrfEta() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc");

    GridDatatype grid = gds.findGridDatatype("T");
    assert grid != null;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.WRFEta : vct.getVerticalTransformType();

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  // TestAll.upcShareDir + /testdata2/grid/netcdf/wrf/wrfout_v2_Lambert.nc

  public void testStride() throws java.io.IOException, InvalidRangeException {
    String filename= TestDir.cdmUnitTestDir + "/conventions/wrf/wrfout_d01_2006-03-08_21-00-00";
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( filename);

    GridDatatype grid = gds.findGridDatatype("T");
    assert grid != null;

    grid = grid.makeSubset(null,null,null,1,2,4);

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    assert shape[0] == 44;
    assert shape[1] == 399/2 + 1;
    assert shape[2] == 399/4 + 1;

    gds.close();
  }
}
