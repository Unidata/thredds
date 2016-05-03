package ucar.nc2.iosp.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Created by rmay on 5/22/15.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribSpheroids {
    String dir = TestDir.cdmUnitTestDir + "formats/grib2/";

    @Test
    public void code0_assume_spherical() throws IOException {
        String filename = dir + "grid174_scanmode_64_example.grb2";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("LatLon_Projection");
            Attribute axis = v.findAttribute("earth_radius");
            Assert.assertEquals(6367470., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }

    @Test
    public void code1_spherical_specified() throws IOException {
        String filename = dir + "LDUE18.grib2";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("LambertConformal_Projection");
            Attribute axis = v.findAttribute("earth_radius");
            Assert.assertEquals(6371200., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }

    // Exercises code path that corrects bad values of earth radius
    @Test
    public void code1_spherical_specified_bad() throws IOException {
        String filename = dir + "sfc_d01_20080430_1200_f00000.grb2";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("LambertConformal_Projection");
            Attribute axis = v.findAttribute("earth_radius");
            Assert.assertEquals(6371200., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }

    @Test
    public void code2_assume_oblate_iau() throws IOException {
        String filename = dir + "MESH_20070326-162126.grib";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("LatLon_Projection");
            Attribute axis = v.findAttribute("semi_major_axis");
            Assert.assertEquals(6378160., axis.getNumericValue().doubleValue(),
                    0.1);
            axis = v.findAttribute("semi_minor_axis");
            Assert.assertEquals(6356684.7, axis.getNumericValue().doubleValue(),
                    0.1); // We use the inverse flattening, not the specified minor
        }
    }

    @Test
    public void code3_oblate_specified_km() throws IOException {
        String filename = dir + "Eumetsat.VerticalPerspective.grb";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("SpaceViewPerspective_Projection");
            Attribute axis = v.findAttribute("semi_major_axis");
            Assert.assertEquals(6378140., axis.getNumericValue().doubleValue(),
                    0.1);
            axis = v.findAttribute("semi_minor_axis");
            Assert.assertEquals(6356755., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }

    @Test
    public void code5_assume_WGS84() throws IOException {
        String filename = dir + "Albers_viirs_s.grb2";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("AlbersEqualArea_Projection");
            Attribute axis = v.findAttribute("semi_major_axis");
            Assert.assertEquals(6378137., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }

    @Test
    public void code6_assume_spherical() throws IOException {
        String filename = dir + "berkes.grb2";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Group grp = ncfile.getRootGroup().getGroups().get(0);
            Variable v = grp.findVariable("LatLon_Projection");
            Attribute axis = v.findAttribute("earth_radius");
            Assert.assertEquals(6371229., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }

    @Test
    public void code7_oblate_specified_m() throws IOException {
        String filename = dir + "TT_FC_INCA.grb2";
        try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
            Variable v = ncfile.findVariable("LambertConformal_Projection");
            Attribute axis = v.findAttribute("semi_major_axis");
            Assert.assertEquals(6377397., axis.getNumericValue().doubleValue(),
                    0.1);
        }
    }
}
