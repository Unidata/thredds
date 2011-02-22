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
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.util.CompareNetcdf;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * test projections
 *
 * @author caron
 */
public class TestProjections extends TestCase {
  private String testDir= TestAll.cdmUnitTestDir + "transforms/";

  public TestProjections(String name) {
    super(name);
  }

  public void testProjections() throws IOException, InvalidRangeException {
    Projection p;

    p = test(testDir + "Sigma_LC.nc",
        "Lambert_Conformal",
        "Temperature",
        LambertConformal.class);

    p = test(testDir + "LambertAzimuth.nc",
        "grid_mapping0",
        "VIL",
        LambertAzimuthalEqualArea.class);

    p = test(testDir+ "PolarStereographic.nc",
        "Polar_Stereographic",
        "D2_O3",
        Stereographic.class);

    p = test(testDir+ "Polar_Stereographic2.nc",
        null,
        "dpd-Surface0",
        Stereographic.class);

    p = test("grid/netcdf/cf/Base_month.nc",
        null,
        "D2_SO4",
        Stereographic.class);

    p = test(testDir+ "Mercator.grib1",
        "Mercator",
        "Temperature",
        Mercator.class);

    p = test(testDir+ "Eumetsat.VerticalPerspective.grb",
        "Space_View_Perspective_or_Orthographic",
        "Pixel_scene_type",
        MSGnavigation.class);
  }

  public void testProjectionsHeiko() throws IOException, InvalidRangeException {
    Projection p;
    String dir = testDir + "heiko/";

    p = test(dir+ "topo_stere_sphere.nc",
        "projection_stere",
        "air_temperature_2m",
        Stereographic.class);

    p = test(dir+ "topo_stere_WGS.nc",
        "projection_stere",
        "air_temperature_2m",
        ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection.class);

    p = test(dir+ "topo_utm_sphere.nc",
        "projection_tmerc",
        "air_temperature_2m",
        ucar.unidata.geoloc.projection.TransverseMercator.class);

    p = test(dir+ "topo_utm_WGS.nc",
        "projection_tmerc",
        "air_temperature_2m",
        ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection.class);
  }

  private Projection test(String filename, String ctvName, String varName, Class projClass) throws IOException, InvalidRangeException {
    System.out.printf("Open= %s%n", filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);

    Variable ctv = null;
    if (ctvName != null) {
      ctv = ncd.findVariable(ctvName);
      assert ctv != null;
      System.out.println(" dump of ctv = \n" + ctv);
    }

    VariableDS v = (VariableDS) ncd.findVariable(varName);
    assert v != null;

    List<CoordinateSystem> cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = cList.get(0);

    List<CoordinateTransform> pList = new ArrayList<CoordinateTransform>();
    List<CoordinateTransform> tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert projClass.isInstance(proj) : proj.getClass().getName();

    VariableDS ctvSyn = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctvSyn);

    if (ctv != null) {
      Formatter f = new Formatter(System.out);
      CompareNetcdf.checkContains(ctv.getAttributes(), ctvSyn.getAttributes(), f);
    }

    ncd.close();
    return proj;
  }

  public void testPSscaleFactor() throws IOException {
    String filename = testDir+ "stereographic/foster.grib2";
    NetcdfDataset ncd = NetcdfDataset.openDataset(filename);
    GridDataset gds = new GridDataset(ncd);
    GridCoordSystem gsys = null;
    ProjectionImpl p = null;

    for (ucar.nc2.dt.GridDataset.Gridset g : gds.getGridsets()) {
      gsys = g.getGeoCoordSystem();
      for (CoordinateTransform t : gsys.getCoordinateTransforms()) {
        if (t instanceof ProjectionCT) {
          p = ((ProjectionCT)t).getProjection();
          break;
        }
      }
    }

    CoordinateAxis1D xaxis = (CoordinateAxis1D) gsys.getXHorizAxis();
    CoordinateAxis1D yaxis =  (CoordinateAxis1D) gsys.getYHorizAxis();
    p.projToLatLon(xaxis.getCoordValue(0), yaxis.getCoordValue(0)  );
    LatLonPointImpl start1 =  p.projToLatLon(xaxis.getCoordValue(0), yaxis.getCoordValue(0));
    LatLonPointImpl start2 =  p.projToLatLon(xaxis.getCoordValue((int)xaxis.getSize()-1), yaxis.getCoordValue((int)yaxis.getSize()-1));
    System.out.printf( "start = %f %f%n", start1.getLatitude(), start1.getLongitude());
    System.out.printf( "end = %f %f%n", start2.getLatitude(), start2.getLongitude());
    
    /*
    wgrib2 /data/laps/lapsprd/gr2/102711000.gr2 -ijlat 358 353 -d 1
      1:0:(358,353),lon=270.784605,lat=41.527360,val=216.094

    wgrib2 /data/laps/lapsprd/gr2/102711000.gr2 -grid -d 11:0:grid_template=20:
	    polar stereographic grid: (358 x 353) input WE|EW:SN output WE:SN res 8
	    North pole lat1 26.023346 lon1 251.023136 latD 34.183360 lonV 259.280944 dx 5000.000000 m dy 5000.000000 m
     */

    assert TestAll.closeEnough(start1.getLatitude(), 26.023346) : TestAll.howClose(start1.getLatitude(), 26.023346);
    assert TestAll.closeEnough(start1.getLongitude(), 251.023136 - 360.0) : TestAll.howClose(start1.getLongitude(), 251.023136- 360.0);

    assert TestAll.closeEnough(start2.getLatitude(), 41.527360,  2.0E-4) :  TestAll.howClose(start2.getLatitude(), 41.527360);
    assert TestAll.closeEnough(start2.getLongitude(), 270.784605 - 360.0, 2.0E-4) : TestAll.howClose(start2.getLongitude(), 270.784605- 360.0);

    ncd.close();
  }
}
