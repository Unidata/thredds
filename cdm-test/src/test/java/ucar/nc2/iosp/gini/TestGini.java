/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.gini;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGini{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Parameterized.Parameters(name="{0}")
    public static Collection giniFiles() {
        Object[][] data = new Object[][] {
                {"n0r_20041013_1852-compress", "Reflectivity", 4736, 3000, -120.0, -80.190548, 23.0, 50.391550},
                {"n0r_20041013_1852-uncompress", "Reflectivity", 4736, 3000, -120.0, -80.190548, 23.0, 50.391550},
                {"n1p_20041206_2140", "Precipitation", 2368, 1500, -120.0, -80.189163, 23.0, 50.3905},
                {"ntp_20041206_2154", "Precipitation", 1184, 750, -120.0, -80.189344, 23.0, 50.390403},
                {"AK-NATIONAL_8km_IR_20050912_2345.gini", "IR", 1012, 874, 174.1623, -114.732224, 19.132, 84.121303},
                {"PR-REGIONAL_4km_12.0_20050922_0600.gini", "IR", 480, 480, -77.0, -58.6254, 9.0, 26.4222},
                {"HI-NATIONAL_10km_SOUND-6.51_20050918_1824.gini", "sounder_imagery", 1472, 1073, 109.9999, -109.1284, -25.0004, 60.6914},
                {"HI-NATIONAL_14km_IR_20050918_2000.gini", "IR", 1012, 737, 109.9999, -109.1285, -25.0004, 60.6443},
                {"HI-REGIONAL_4km_IR_20050919_1315.gini", "IR", 560, 520, -167.315, -145.878, 9.343, 28.0922},
                {"SUPER-NATIONAL_1km_PW_20050923_1400.gini", "PW", 1536, 1008, -141.0274, -32.417681, 7.8381, 79.760853},
                {"SUPER-NATIONAL_1km_SFC-T_20050912_1900.gini", "SFC_T", 1536, 1008, -141.0274, -32.417681, 7.8381, 79.760853},
                {"SUPER-NATIONAL_8km_IR_20050911_2345.gini", "IR", 1536, 1008, -141.0274, -32.54069, 7.8381, 79.679395},
                {"EAST-CONUS_4km_12.0_20050912_0600.gini", "IR", 1280, 1280, -113.1333, -68.314380, 16.3691, 63.081454},
                {"EAST-CONUS_8km_13.3_20050912_2240.gini", "IR", 640, 640, -113.1333, -68.348871, 16.3691, 63.045506},
                {"WEST-CONUS_4km_3.9_20050912_2130.gini", "IR", 1100, 1280, -133.4588, -94.225514, 12.19, 58.902354}
        };
        return Arrays.asList(data);
    }

    String fname, varName;
    int xSize, ySize;
    double min_lon, max_lon, min_lat, max_lat;

    public TestGini(String fname, String varName, int nx, int ny,
                    double min_lon, double max_lon, double min_lat, double max_lat)
    {
        this.fname = fname;
        this.varName = varName;
        this.xSize = nx;
        this.ySize = ny;
        this.min_lon = min_lon;
        this.max_lon = max_lon;
        this.min_lat = min_lat;
        this.max_lat = max_lat;
    }

    @Test
    public void testGiniRead() throws IOException {
        try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/gini/" + fname)) {
            Variable v = ncfile.findVariable(varName);

            // Make sure we can get the expected variable and that it is at-least 2D
            Assert.assertNotNull(v);
            Assert.assertNotNull(v.getDimension(0));
            Assert.assertNotNull(v.getDimension(1));

            // Make sure the variable has a grid mapping set and that it points
            // to a valid variable
            Attribute grid_mapping = v.findAttribute("grid_mapping");
            Assert.assertNotNull(grid_mapping);
            Variable proj_var = ncfile.findVariable(grid_mapping.getStringValue());
            Assert.assertNotNull(proj_var);
            Assert.assertNotNull(proj_var.findAttribute("grid_mapping_name"));

            // Check size
            Assert.assertEquals(ySize,
                    v.getDimension(v.findDimensionIndex("y")).getLength());
            Assert.assertEquals(xSize,
                    v.getDimension(v.findDimensionIndex("x")).getLength());

            // Check projection info
            Assert.assertEquals(min_lon,
                    ncfile.findAttribute("@geospatial_lon_min").getNumericValue().doubleValue(),
                    1e-6);
            Assert.assertEquals(max_lon,
                    ncfile.findAttribute("@geospatial_lon_max").getNumericValue().doubleValue(),
                    1e-6);
            Assert.assertEquals(min_lat,
                    ncfile.findAttribute("@geospatial_lat_min").getNumericValue().doubleValue(),
                    1e-6);
            Assert.assertEquals(max_lat,
                    ncfile.findAttribute("@geospatial_lat_max").getNumericValue().doubleValue(),
                    1e-6);

            // Read the array and check that its size matches the variable's
            Array a = v.read();
            Assert.assertNotNull(a);
            Assert.assertEquals(v.getSize(), a.getSize());

            // For byte data, make sure it is specified as unsigned and
            // check that the actual number of bytes is proper
            if (v.getDataType() == DataType.BYTE) {
                byte[] arr = (byte[])a.getStorage();
                Assert.assertEquals(v.getSize(), arr.length);
                Assert.assertNotNull(v.findAttribute(CDM.UNSIGNED));
            }
        }
    }
}
