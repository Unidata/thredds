package ucar.nc2.grib;

import org.junit.Test;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Created by rmay on 3/2/16.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribUnits {

    @Test
    public void test_ordered_sequence_units() throws IOException {
        // Make sure we return the udunits string of "count"
        String filename = "tds/ncep/WW3_Coastal_Alaska_20140804_0000.grib2";
        try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + filename)) {
            Variable var = ds.findVariable(null, "ordered_sequence_of_data");
            Attribute att = var.findAttribute("units");
            Assert.assertNotNull(att);
            Assert.assertEquals("count", att.getStringValue());
        }
    }

    @Test
    public void test_true_degrees() throws IOException {
        // Make sure we return grib units of "degree true" as "degree_true"
        String filename = "tds/ncep/NDFD_CONUS_5km_20140805_1200.grib2";
        try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + filename)) {
            Group grp = ds.findGroup("LambertConformal_1377X2145-38p22N-95p43W-2");
            Assert.assertNotNull(grp);

            Variable var = grp.findVariable("Wind_direction_from_which_blowing_height_above_ground");
            Assert.assertNotNull(var);

            Attribute att = var.findAttribute("units");
            Assert.assertNotNull(att);
            Assert.assertEquals("degree_true", att.getStringValue());
        }
    }

    @Test
    public void test_code_table() throws IOException {
        // Make sure we don't add '.' to "Code table a.b.c"
        String filename = "tds/ncep/NDFD_CONUS_5km_20140805_1200.grib2";
        try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + filename)) {
            Group grp = ds.findGroup("LambertConformal_1377X2145-38p22N-95p43W");
            Assert.assertNotNull(grp);

            Variable var = grp.findVariable("Categorical_Rain_surface");
            Assert.assertNotNull(var);

            Attribute att = var.findAttribute("units");
            Assert.assertNotNull(att);
            Assert.assertEquals("Code table 4.222", att.getStringValue());
        }
    }
}
