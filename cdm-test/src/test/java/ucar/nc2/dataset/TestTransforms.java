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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.vertical.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * test vertical transforms.
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestTransforms {
  private String testDir= TestDir.cdmUnitTestDir + "transforms/";

  @Test
  public void testHybridSigmaPressure() throws IOException, InvalidRangeException {
    String filename = testDir + "HybridSigmaPressure.nc";
    test(filename, "lev", "T", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testHybridSigmaPressure2() throws IOException, InvalidRangeException {
    String filename = testDir +  "climo.cam2.h0.0000-09.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VerticalTransform vt = test(ncd, "lev", "T", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class, SimpleUnit.pressureUnit, true);

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

  @Test
  public void testHybridSigmaPressure3() throws IOException, InvalidRangeException {
    String filename = testDir +  "HIRLAMhybrid.ncml";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VerticalTransform vt = test(ncd, "hybrid", "Relative_humidity_hybrid", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class,
        SimpleUnit.pressureUnit, true);

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

  @Test
  public void testOceanS() throws IOException, InvalidRangeException {
    String filename = testDir+ "OceanS.nc";
    test(filename, "s_rho", "salt", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testOceanS2() throws IOException, InvalidRangeException {
    String filename = testDir+ "OceanS2.nc";
    test(filename, "s_rho", "temp", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testOceanSigma() throws IOException, InvalidRangeException {
    String filename = testDir+ "OceanSigma.nc";
    test(filename, "zpos", "salt", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testOceanSigma2() throws IOException, InvalidRangeException {
    String filename = testDir+ "erie_test.ncml";
    test(filename, "sigma", "temp", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testGomoos() throws IOException, InvalidRangeException {
    String filename = testDir+ "gomoos.ncml";
    test(filename, "zpos", "temp", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testWrf() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/global.nc";
    test(filename, "z", "T", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "U", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "V", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z_stag", "W", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testWrf2() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_mercator.nc";
    test(filename, "z", "T", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "U", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z", "V", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.pressureUnit);
    test(filename, "z_stag", "W", "Time", VerticalCT.Type.WRFEta, WRFEta.class, SimpleUnit.meterUnit);
  }

  // LOOK these are failing
  // needs COnvention = CF
  public void testOceanSigmaNcml() throws IOException, InvalidRangeException {
    String filename = "http://coast-enviro.er.usgs.gov/models/share/glos_test.ncml";
    test(filename, "sigma", "temp", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  /* btestOceanS3
  problem is that u is

   float u(ocean_time=1, s_rho=6, eta_u=120, xi_u=155);
     :coordinates = "lon_u lat_u s_rho ocean_time";

    double s_rho(s_rho=6);
     :long_name = "S-coordinate at RHO-points";
     :positive = "up";
     :standard_name = "ocean_s_coordinate";
     :formula_terms = "s: s_rho eta: zeta depth: h a: theta_s b: theta_b depth_c: hc";

     which uses zeta:
        float zeta(ocean_time=1, eta_rho=120, xi_rho=156);

     which is 120 x 126 instead of 120 x 125.

     seems to be an rsignell file. may be motivation for staggered convention

 OceanS_Transform_s_rho type=Vertical
    standard_name = ocean_s_coordinate
    formula_terms = s: s_rho eta: zeta depth: h a: theta_s b: theta_b depth_c: hc
    height_formula = height(x,y,z) = depth_c*s(z) + (depth(x,y)-depth_c)*C(z) + eta(x,y) * (1 + (depth_c*s(z) + (depth(x,y)-depth_c)*C(z))/depth(x,y)
    C_formula = C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)
    Eta_variableName = zeta
    S_variableName = s_rho
    Depth_variableName = h
    Depth_c_variableName = hc
    A_variableName = theta_s
    B_variableName = theta_b

   */
  @Test
  public void btestOceanS3() throws IOException, InvalidRangeException {
    String filename = testDir+ "ocean_his.nc";
    _test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit, false);
  }

  @Test
  public void btestOceanG1() throws IOException, InvalidRangeException {
    String filename = testDir+ "ocean_his_g1.nc";
    _test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanSG1, OceanSG1.class, SimpleUnit.meterUnit, false);
  }

  @Test
  public void btestOceanG2() throws IOException, InvalidRangeException {
    String filename = testDir+ "ocean_his_g2.nc";
    _test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanSG2, OceanSG2.class, SimpleUnit.meterUnit, false);
  }

  @Test
  public void testSigma() throws IOException, InvalidRangeException {
    String filename = testDir+ "Sigma_LC.nc";
    test(filename, "level", "Temperature", null, VerticalCT.Type.Sigma, AtmosSigma.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testExisting3D() throws IOException, InvalidRangeException {
    String filename = testDir+ "VExisting3D_NUWG.nc";
    test(filename, "VerticalTransform", "rhu_hybr", "record", VerticalCT.Type.Existing3DField, VTfromExistingData.class,
        null);
  }

  private VerticalTransform test(String filename, String levName, String varName, String timeName,
                                 VerticalCT.Type vtype, Class vclass, SimpleUnit unit)
      throws IOException, InvalidRangeException {

    return _test(filename, levName, varName, timeName, vtype, vclass, unit, true);
  }

  private VerticalTransform _test(String filename, String levName, String varName, String timeName,
                                 VerticalCT.Type vtype, Class vclass, SimpleUnit unit, boolean varsMatch)
      throws IOException, InvalidRangeException {

    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    test(ncd, levName, varName, timeName, vtype, vclass, unit, varsMatch);
    ncd.close();

    if (varsMatch) testGrid(filename, varName);
    return null;
  }

  private VerticalTransform test(NetcdfDataset ncd, String levName, String varName, String timeName,
                                 VerticalCT.Type vtype, Class vclass, SimpleUnit vunit, boolean varsMatch)
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
    assert vList.size() == 1 : vList.size();
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
        System.out.printf("%s: varSection shape = %s %n", v.getFullName(), varSection);
        System.out.printf("%s: coordVal shape = %s %n", v.getFullName(), cSection);
        if (varSection.computeSize() != cSection.computeSize())
          System.out.println("HEY");
        if (varsMatch) assert varSection.computeSize() == cSection.computeSize();
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
