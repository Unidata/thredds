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

import ucar.nc2.TestAll;
import ucar.nc2.*;
import ucar.unidata.geoloc.vertical.*;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.*;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * test various transforms that we have test data for.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class TestTransforms extends TestCase {

  public TestTransforms(String name) {
    super(name);
  }

  public void testHybridSigmaPressure() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/HybridSigmaPressure.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("lev");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("T");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    assert tList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) tList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == VerticalCT.Type.HybridSigmaPressure;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    Dimension timeDim = ncd.findDimension("time");
    VerticalTransform vt = vct.makeVerticalTransform(ncd, timeDim);
    assert vt != null;
    assert vt instanceof HybridSigmaPressure;

    assert vt.getUnitString().equals("Pa");
    assert vt.isTimeDependent();

    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
    }

    ncd.close();
  }

  public void testHybridSigmaPressure2() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/netcdf/cf/climo.cam2.h0.0000-09.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("lev");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("T");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    assert tList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) tList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == VerticalCT.Type.HybridSigmaPressure;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    Dimension timeDim = ncd.findDimension("time");
    VerticalTransform vt = vct.makeVerticalTransform(ncd, timeDim);
    assert vt != null;
    assert vt instanceof HybridSigmaPressure;

    assert vt.getUnitString().equals("Pa");
    assert vt.isTimeDependent();

    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
      int[] shape = coordVals.getShape();
      assert shape[0] == ncd.findDimension("lev").getLength();
      assert shape[1] == ncd.findDimension("lat").getLength();
      assert shape[2] == ncd.findDimension("lon").getLength();
    }

    ncd.close();
  }

  public void testOceanS() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/OceanS.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("s_rho");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("salt");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    assert tList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) tList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == VerticalCT.Type.OceanS;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    Dimension timeDim = ncd.findDimension("ocean_time");
    assert null != timeDim;
    VerticalTransform vt = vct.makeVerticalTransform(ncd, timeDim);
    assert vt != null;
    assert vt instanceof OceanS;

    assert vt.getUnitString().equals("meter") : vt.getUnitString();
    assert vt.isTimeDependent();

    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
    }

    ncd.close();
  }


  public void testOceanSigma() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/OceanSigma.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("zpos");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("salt");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    assert tList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) tList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == VerticalCT.Type.OceanSigma;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    Dimension timeDim = ncd.findDimension("time");
    assert null != timeDim;
    VerticalTransform vt = vct.makeVerticalTransform(ncd, timeDim);
    assert vt != null;
    assert vt instanceof OceanSigma;

    assert vt.getUnitString().equals("meters") : vt.getUnitString();
    assert vt.isTimeDependent();

    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
    }
    ncd.close();
  }

  public void testSigma() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/Sigma_LC.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("level");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Temperature");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List vList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Vertical)
        vList.add(ct);
    }
    assert vList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) vList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == VerticalCT.Type.Sigma;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    VerticalTransform vt = vct.makeVerticalTransform(ncd, null);
    assert vt != null;
    assert vt instanceof AtmosSigma;

    assert vt.getUnitString().equals("mbar") : vt.getUnitString();
    assert !vt.isTimeDependent();

    ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(0);

    ncd.close();
  }

  public void testExisting() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/VExisting3D_NUWG.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("VerticalTransform");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("rhu_hybr");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    assert tList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) tList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == VerticalCT.Type.Existing3DField;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    Dimension timeDim = ncd.findDimension("record");
    assert null != timeDim;
    VerticalTransform vt = vct.makeVerticalTransform(ncd, timeDim);
    assert vt != null;
    assert vt instanceof VTfromExistingData;

    assert vt.getUnitString().equals("gp m") : vt.getUnitString();
    assert vt.isTimeDependent();

    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
    }
    ncd.close();
  }

  public void testLC() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/Sigma_LC.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Lambert_Conformal");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Temperature");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof LambertConformal;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testLA() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/LambertAzimuth.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("grid_mapping0");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("VIL");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof LambertAzimuthalEqualArea;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testPS() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/PolarStereographic.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Polar_Stereographic");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("D2_O3");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Stereographic;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testPS2() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/Polar_Stereographic2.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);

    VariableDS v = (VariableDS) ncd.findVariable("dpd-Surface0");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Stereographic;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testPS3() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/netcdf/cf/Base_month.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);

    VariableDS v = (VariableDS) ncd.findVariable("D2_SO4");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Stereographic;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testMercator() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/Mercator.grib1";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Mercator_Projection_Grid");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Temperature");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Mercator : proj.getClass().getName();

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testVerticalPerspective() throws IOException, InvalidRangeException {
    String filename = TestAll.upcShareTestDataDir + "grid/transforms/Eumetsat.VerticalPerspective.grb";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Space_View_Perspective_or_Orthographic");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Pixel_scene_type");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof VerticalPerspectiveView : proj.getClass().getName();

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

}
