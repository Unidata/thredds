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

import ucar.nc2.TestAll;
import ucar.nc2.*;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.vertical.*;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.ArrayDouble;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * test vertical transforms.
 *
 * @author caron
 */
public class TestTransforms extends TestCase {
  private String testDir="cdmUnitTest/transforms/";

  public TestTransforms(String name) {
    super(name);
  }

  public void testHybridSigmaPressure() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir + "HybridSigmaPressure.nc";
    test(filename, "lev", "T", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class,
        SimpleUnit.pressureUnit);
  }

  public void testHybridSigmaPressure2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/netcdf/cf/climo.cam2.h0.0000-09.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VerticalTransform vt = test(ncd, "lev", "T", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class,
        SimpleUnit.pressureUnit);

    Dimension timeDim = ncd.findDimension("time");
    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
      int[] shape = coordVals.getShape();
      assert shape[0] == ncd.findDimension("lev").getLength();
      assert shape[1] == ncd.findDimension("lat").getLength();
      assert shape[2] == ncd.findDimension("lon").getLength();
    }

    ncd.close();
  }

  public void testHybridSigmaPressure3() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/grib/grib1/ecmwf/HIRLAMhybrid.ncml";
    //String filename =   "/local/robb/data/grib/hybrid/HIRLAMhybrid.ncml";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VerticalTransform vt = test(ncd, "hybrid", "Relative_humidity", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class,
        SimpleUnit.pressureUnit);

    Dimension timeDim = ncd.findDimension("time");
    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
      int[] shape = coordVals.getShape();
      assert shape[0] == ncd.findDimension("hybrid").getLength();
      assert shape[1] == ncd.findDimension("y").getLength();
      assert shape[2] == ncd.findDimension("x").getLength();
    }

    ncd.close();
  }

  public void testOceanS() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "OceanS.nc";
    test(filename, "s_rho", "salt", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  public void testOceanS2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "OceanS2.nc";
    test(filename, "s_rho", "temp", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  public void testOceanSigma() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "OceanSigma.nc";
    test(filename, "zpos", "salt", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  public void testOceanSigma2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "erie_test.ncml";
    test(filename, "sigma", "temp", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  public void testGomoos() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "gomoos.ncml";
    test(filename, "zpos", "temp", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  public void testWrf() throws IOException, InvalidRangeException {
    String filename = TestAll.cdmUnitTestDir + "wrf/global.nc";
    test(filename, "z", "T", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "U", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "V", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z_stag", "W", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.meterUnit);
  }

  public void testWrf2() throws IOException, InvalidRangeException {
    String filename = TestAll.cdmUnitTestDir + "wrf/wrfout_mercator.nc";
    test(filename, "z", "T", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "U", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "V", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z_stag", "W", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.meterUnit);
  }

  // LOOK these are failing
  public void btestOceanSigmaNcml() throws IOException, InvalidRangeException {
    String filename = "http://coast-enviro.er.usgs.gov/models/share/glos_test.ncml";
    test(filename, "sigma", "temp", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  public void btestOceanS3() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "ocean_his.nc";
    test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  public void btestOceanG1() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "ocean_his_g1.nc";
    test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanSG1, OceanSG1.class, SimpleUnit.meterUnit);
  }

  public void btestOceanG2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "ocean_his_g2.nc";
    test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanSG2, OceanSG2.class, SimpleUnit.meterUnit);
  }

  public void testSigma() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "Sigma_LC.nc";
    test(filename, "level", "Temperature", null, VerticalCT.Type.Sigma, AtmosSigma.class, SimpleUnit.pressureUnit);
  }

  public void testExisting3D() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + testDir+ "VExisting3D_NUWG.nc";
    test(filename, "VerticalTransform", "rhu_hybr", "record", VerticalCT.Type.Existing3DField, VTfromExistingData.class,
        null);
  }

  private VerticalTransform test(String filename, String levName, String varName, String timeName,
                                 VerticalCT.Type vtype, Class vclass, SimpleUnit unit)
      throws IOException, InvalidRangeException {

    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    test(ncd, levName, varName, timeName, vtype, vclass, unit);
    ncd.close();

    testGrid(filename, varName);
    return null;
  }

  private VerticalTransform test(NetcdfDataset ncd, String levName, String varName, String timeName,
                                 VerticalCT.Type vtype, Class vclass, SimpleUnit vunit)
      throws IOException, InvalidRangeException {

    System.out.printf("file= %s%n", ncd.getLocation());

    VariableDS lev = (VariableDS) ncd.findVariable(levName);
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable(varName);
    assert v != null;
    System.out.printf(" data variable = %s%n", v);
    Section varSection = new Section(v.getShapeAsSection());

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List<CoordinateTransform> vList = new ArrayList<CoordinateTransform>();
    for (CoordinateTransform ct : csys.getCoordinateTransforms()) {
      if (ct.getTransformType() == TransformType.Vertical)
        vList.add(ct);
    }
    assert vList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) vList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == vtype : vct.getVerticalTransformType();

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    VerticalTransform vt = null;
    if (timeName == null) {
      vt = vct.makeVerticalTransform(ncd, null);
      assert !vt.isTimeDependent();
      ucar.ma2.Array coordVals = vt.getCoordinateArray(0);
      assert (null != coordVals);

      Section cSection = new Section(coordVals.getShape());
      System.out.printf(" coordVal shape = %s %n", cSection);
      assert varSection.computeSize() == cSection.computeSize();

    } else {
      Dimension timeDim = ncd.findDimension(timeName);
      assert null != timeDim;
      vt = vct.makeVerticalTransform(ncd, timeDim);
      assert vt.isTimeDependent();

      varSection = varSection.removeRange(0); // remove time dependence for comparision

      for (int i = 0; i < timeDim.getLength(); i++) {
        ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
        assert (null != coordVals);
        Section cSection = new Section(coordVals.getShape());
        System.out.printf("%s: varSection shape = %s %n", v.getName(), varSection);
        System.out.printf("%s: coordVal shape = %s %n", v.getName(), cSection);
        assert varSection.computeSize() == cSection.computeSize();
      }
    }
    assert vt != null;
    assert vclass.isInstance(vt);

    // should be compatible with vunit
    if (vunit != null) {
      String vertCoordUnit = vt.getUnitString();
      assert vunit.isCompatible(vertCoordUnit) : vertCoordUnit + " not udunits compatible with " + vunit.getUnitString();
    }

    return vt;
  }

  private void testGrid(String uri, String var) throws IOException, InvalidRangeException {

    GridDataset ds = null;
    try {
      ds = GridDataset.open(uri);
      GeoGrid grid = ds.findGridByName(var);
      Section s = new Section(grid.getShape());
      System.out.printf("var = %s %n", s);

      GridCoordSystem GridCoordS = grid.getCoordinateSystem();
      VerticalTransform vt = GridCoordS.getVerticalTransform();
      ArrayDouble.D3 z = vt.getCoordinateArray(0);
      Section sv = new Section(z.getShape());
      System.out.printf("3dcoord = %s %n", sv);

      if (vt.isTimeDependent())
        s = s.removeRange(0);
      assert s.equals(sv);
    } finally {
      if (ds != null) ds.close();
    }
  }

}
