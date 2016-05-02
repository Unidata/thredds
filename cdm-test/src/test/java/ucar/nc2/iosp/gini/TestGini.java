/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.iosp.gini;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGini{

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
