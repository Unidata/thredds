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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;
import ucar.unidata.geoloc.projection.proj4.EquidistantAzimuthalProjection;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.*;

/**
 * test projections
 *
 * @author caron
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestProjections {
  private static String testDir= TestDir.cdmUnitTestDir + "transforms/";
  private static LatLonPointImpl testPoint = new LatLonPointImpl(0, 145.0);

  @Parameterized.Parameters(name="{0}-{1}")
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][]{

            {testDir + "Sigma_LC.nc", "Lambert_Conformal", "Temperature", LambertConformal.class, null},

            {testDir + "LambertAzimuth.nc", "grid_mapping0", "VIL", LambertAzimuthalEqualArea.class, null},

            {testDir + "PolarStereographic.nc", "Polar_Stereographic", "D2_O3", Stereographic.class, null},

            {testDir + "Polar_Stereographic2.nc", null, "dpd-Surface0", Stereographic.class, null},

            {testDir + "Base_month.nc", null, "D2_SO4", Stereographic.class, null},

            {testDir + "Mercator.grib1", "Mercator_Projection", "Temperature_isobaric", Mercator.class, null},

            {testDir + "Eumetsat.VerticalPerspective.grb", "SpaceViewPerspective_Projection", "Pixel_scene_type", MSGnavigation.class, testPoint},

            {testDir + "sinusoidal/MOD13Q1.A2008033.h12v04.005.2008051065305.hdf",
                    "MODIS_Grid_16DAY_250m_500m_VI/Data_Fields/Projection",
                    "MODIS_Grid_16DAY_250m_500m_VI/Data_Fields/250m_16_days_NDVI",
                    Sinusoidal.class, testPoint},

            {testDir + "heiko/topo_stere_sphere.nc",
                    "projection_stere",
                    "air_temperature_2m",
                    Stereographic.class, null},

            {testDir + "heiko/topo_stere_WGS.nc",
                    "projection_stere",
                    "air_temperature_2m",
                    ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection.class, null},

            {testDir + "heiko/topo_utm_sphere.nc",
                    "projection_tmerc",
                    "air_temperature_2m",
                    ucar.unidata.geoloc.projection.TransverseMercator.class, null},

            {testDir + "heiko/topo_utm_WGS.nc",
                    "projection_tmerc",
                    "air_temperature_2m",
                    ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection.class, null},

            {testDir + "rotatedPole/snow.DMI.ecctrl.ncml",
                    "rotated_pole",
                    "snow",
                    RotatedPole.class, null},

            {testDir + "melb-small_LCEA.nc",
                    "lambert_cylindrical_equal_area",
                    "Band1",
                    CylindricalEqualAreaProjection.class, testPoint},

            {testDir + "melb-small_AZE.nc",
                    "azimuthal_equidistant",
                    "Band1",
                    EquidistantAzimuthalProjection.class, new LatLonPointImpl(-37, 145.0)},

            //  :sweep_angle_axis = "x";
            // :longitude_of_projection_origin = -75.0; covers western hemisphere
            {testDir + "geostationary/IT_ABI-L2-CMIPF-M3C16_G16_s2005155201500_e2005155203700_c2014058132255.nc",
                    "goes_imager_projection",
                    "CMI",
                    Geostationary.class, new LatLonPointImpl(-37, -45.0)},

    };

    return Arrays.asList(data);
  }


  String filename;
  String ctvName;
  String varName;
  Class projClass;
  LatLonPointImpl testPt;

  public TestProjections(String filename, String ctvName, String varName, Class projClass, LatLonPointImpl testPt) {
    this.filename = filename;
    this.ctvName = ctvName;
    this.varName = varName;
    this.projClass = projClass;
    this.testPt = testPt;
  }

  @Test
  public void testOneProjection() throws IOException, InvalidRangeException {
    System.out.printf("Open= %s%n", filename);
    try (NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename)) {

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

      List<CoordinateTransform> pList = new ArrayList<>();
      List<CoordinateTransform> tList = csys.getCoordinateTransforms();
      assert tList != null;
      for (CoordinateTransform ct : tList) {
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

      if (projClass != RotatedPole.class) {
        System.out.printf("Projection Parameters%n");
        boolean found = false;
        double radius = 0.0;
        for (Parameter p : proj.getProjectionParameters()) {
          System.out.printf("%s%n", p);
          if (p.getName().equals(CF.EARTH_RADIUS)) {
            found = true;
            radius = p.getNumericValue();
          }
          if (p.getName().equals(CF.SEMI_MAJOR_AXIS)) {
            found = true;
            radius = p.getNumericValue();
          }
        }
        System.out.printf("%n");

        assert found;
        assert (radius > 10000) : radius; // meters
      }

      VariableDS ctvSyn = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
      System.out.println(" dump of equivilent ctv = \n" + ctvSyn);

      if (ctv != null) {
        Formatter f = new Formatter(System.out);
        ucar.unidata.util.test.CompareNetcdf.checkContains(ctv.getAttributes(), ctvSyn.getAttributes(), f);
      }

      if (testPt != null) {
        ProjectionPoint pt =  proj.latLonToProj(testPt, new ProjectionPointImpl());
        assert pt != null;
        assert !Double.isNaN( pt.getX());
        assert !Double.isNaN(pt.getY());
      }

    }
  }

}
