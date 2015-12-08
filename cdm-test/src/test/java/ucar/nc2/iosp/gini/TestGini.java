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
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGini{

    @Parameterized.Parameters(name="{0}")
    public static Collection giniFiles() {
        Object[][] data = new Object[][] {
                {"n0r_20041013_1852-compress", "Reflectivity"},
                {"n0r_20041013_1852-uncompress", "Reflectivity"},
                {"n1p_20041206_2140", "Precipitation"},
                {"ntp_20041206_2154", "Precipitation"},
                {"AK-NATIONAL_8km_IR_20050912_2345.gini", "IR"},
                {"PR-REGIONAL_4km_12.0_20050922_0600.gini", "IR"},
                {"HI-NATIONAL_10km_SOUND-6.51_20050918_1824.gini", "sounder_imagery"},
                {"HI-NATIONAL_14km_IR_20050918_2000.gini", "IR"},
                {"HI-REGIONAL_4km_IR_20050919_1315.gini", "IR"},
                {"SUPER-NATIONAL_1km_PW_20050923_1400.gini", "PW"},
                {"SUPER-NATIONAL_1km_SFC-T_20050912_1900.gini", "SFC_T"},
                {"SUPER-NATIONAL_8km_IR_20050911_2345.gini", "IR"},
                {"EAST-CONUS_4km_12.0_20050912_0600.gini", "IR"},
                {"EAST-CONUS_8km_13.3_20050912_2240.gini", "IR"},
                {"WEST-CONUS_4km_3.9_20050912_2130.gini", "IR"}
        };
        return Arrays.asList(data);
    }

    String fname, varName;

    public TestGini(String fname, String varName)
    {
        this.fname = fname;
        this.varName = varName;
    }

    @Test
    public void testGiniRead() throws IOException {
        try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/gini/" + fname)) {
            Variable v = ncfile.findVariable(varName);

            // Make sure we can get the expected variable and that it is 2D
            Assert.assertNotNull(v);
            Assert.assertNotNull(v.getDimension(0));
            Assert.assertNotNull(v.getDimension(1));

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
