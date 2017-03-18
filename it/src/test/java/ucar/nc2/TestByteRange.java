/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2;

import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Test that client side and server side byte range access works.
 */
public class TestByteRange extends UnitTestCommon
{
    // Collect testcases locally
    static public class Testcase
    {
        String title;
        String url;
        String cdl;

        public Testcase(String title, String url)
        {
            this.title = title;
            this.url = url;
        }
    }

    String testserver = null;
    List<Testcase> testcases = null;

    public TestByteRange()
    {
        super("ByteRange tests");
        definetestcases();
    }

    void
    definetestcases()
    {
        String threddsRoot = getThreddsroot();
        testcases = new ArrayList<Testcase>();
        testcases.add(new Testcase("TestByteRanges",
                "http://localhost:8081/thredds/fileServer/localContent/testData.nc"
                // "http://localhost:8081/thredds/fileServer/scanLocal/sss_binned_L3_MON_SCI_V4.0_2011.nc"
                //"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc"
        ));
    }

    @Test
    public void
    testByteRange() throws Exception
    {
        System.out.println("TestByteRange:");
        for(Testcase testcase : testcases) {
            System.out.println("url: " + testcase.url);
            process1(testcase);
        }
    }

    void process1(Testcase testcase)
            throws Exception
    {
        NetcdfFile ncfile = NetcdfFile.open(testcase.url);
        if(ncfile == null)
            throw new Exception("Cannot read: " + testcase.url);
        StringWriter ow = new StringWriter();
        PrintWriter pw = new PrintWriter(ow);
        ncfile.writeCDL(pw, true);
        pw.close();
        ow.close();
        String captured = ow.toString();
        if(prop_visual)
            visual(testcase.title, captured);
        if(prop_diff) {
            Assert.assertTrue("***Fail: Files are different",
                    same(getTitle(), testData_baseline, captured));
        }
    }

    //////////////////////////////////////////////////
    // baseline

    String testData_baseline =
            "netcdf http\\://localhost\\:8081/thredds/fileServer/localContent/testData {\n"
                    + "  dimensions:\n"
                    + "    record = UNLIMITED;   // (1 currently)\n"
                    + "    x = 135;\n"
                    + "    y = 95;\n"
                    + "    datetime_len = 21;\n"
                    + "    nmodels = 1;\n"
                    + "    ngrids = 1;\n"
                    + "    nav = 1;\n"
                    + "    nav_len = 100;\n"
                    + "  variables:\n"
                    + "    double reftime(record);\n"
                    + "      string reftime:long_name = \"reference time\";\n"
                    + "      string reftime:units = \"hours since 1992-1-1\";\n"
                    + "\n"
                    + "    double valtime(record);\n"
                    + "      string valtime:long_name = \"valid time\";\n"
                    + "      string valtime:units = \"hours since 1992-1-1\";\n"
                    + "\n"
                    + "    char datetime(record, datetime_len);\n"
                    + "      string datetime:long_name = \"reference date and time\";\n"
                    + "\n"
                    + "    float valtime_offset(record);\n"
                    + "      string valtime_offset:long_name = \"hours from reference time\";\n"
                    + "      string valtime_offset:units = \"hours\";\n"
                    + "\n"
                    + "    int model_id(nmodels);\n"
                    + "      string model_id:long_name = \"generating process ID number\";\n"
                    + "\n"
                    + "    char nav_model(nav, nav_len);\n"
                    + "      string nav_model:long_name = \"navigation model name\";\n"
                    + "\n"
                    + "    int grid_type_code(nav);\n"
                    + "      string grid_type_code:long_name = \"GRIB-1 GDS data representation type\";\n"
                    + "\n"
                    + "    char grid_type(nav, nav_len);\n"
                    + "      string grid_type:long_name = \"GRIB-1 grid type\";\n"
                    + "\n"
                    + "    char grid_name(nav, nav_len);\n"
                    + "      string grid_name:long_name = \"grid name\";\n"
                    + "\n"
                    + "    int grid_center(nav);\n"
                    + "      string grid_center:long_name = \"GRIB-1 originating center ID\";\n"
                    + "\n"
                    + "    int grid_number(nav, ngrids);\n"
                    + "      string grid_number:long_name = \"GRIB-1 catalogued grid numbers\";\n"
                    + "      grid_number:_FillValue = -9999;\n"
                    + "\n"
                    + "    char x_dim(nav, nav_len);\n"
                    + "      string x_dim:long_name = \"x dimension name\";\n"
                    + "\n"
                    + "    char y_dim(nav, nav_len);\n"
                    + "      string y_dim:long_name = \"y dimension name\";\n"
                    + "\n"
                    + "    int Nx(nav);\n"
                    + "      string Nx:long_name = \"number of points along x-axis\";\n"
                    + "\n"
                    + "    int Ny(nav);\n"
                    + "      string Ny:long_name = \"number of points along y-axis\";\n"
                    + "\n"
                    + "    float La1(nav);\n"
                    + "      string La1:long_name = \"latitude of first grid point\";\n"
                    + "      string La1:units = \"degrees_north\";\n"
                    + "\n"
                    + "    float Lo1(nav);\n"
                    + "      string Lo1:long_name = \"longitude of first grid point\";\n"
                    + "      string Lo1:units = \"degrees_east\";\n"
                    + "\n"
                    + "    float Lov(nav);\n"
                    + "      string Lov:long_name = \"orientation of the grid\";\n"
                    + "      string Lov:units = \"degrees_east\";\n"
                    + "\n"
                    + "    float Dx(nav);\n"
                    + "      string Dx:long_name = \"x-direction grid length\";\n"
                    + "      string Dx:units = \"km\";\n"
                    + "\n"
                    + "    float Dy(nav);\n"
                    + "      string Dy:long_name = \"y-direction grid length\";\n"
                    + "      string Dy:units = \"km\";\n"
                    + "\n"
                    + "    byte ProjFlag(nav);\n"
                    + "      string ProjFlag:long_name = \"projection center flag\";\n"
                    + "\n"
                    + "    byte ResCompFlag(nav);\n"
                    + "      string ResCompFlag:long_name = \"resolution and component flags\";\n"
                    + "\n"
                    + "    float Z_sfc(record, y, x);\n"
                    + "      string Z_sfc:long_name = \"Geopotential height, gpm\";\n"
                    + "      string Z_sfc:units = \"gp m\";\n"
                    + "      Z_sfc:_FillValue = -9999.0f;\n"
                    + "      string Z_sfc:navigation = \"nav\";\n"
                    + "\n"
                    + "  // global attributes:\n"
                    + "  string :record = \"reftime, valtime\";\n"
                    + "  string :history = \"2003-09-25 16:09:26 - created by gribtocdl 1.4 - 12.12.2002\";\n"
                    + "  string :title = \"CMC_reg_HGT_SFC_0_ps60km_2003092500_P000.grib\";\n"
                    + "  string :Conventions = \"NUWG\";\n"
                    + "  :version = 0.0;\n"
                    + "}\n";

}
