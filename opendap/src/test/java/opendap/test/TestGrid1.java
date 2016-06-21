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
package opendap.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * Test nc2 dods in the JUnit framework.
 * Dataset {
 * Grid {
 * ARRAY:
 * Float32 var[lat=2][lon=2];
 * MAPS:
 * Float32 lat[lat=2];
 * Float32 lon[lon=2];
 * } testgrid1
 * data:
 * var = 0.0, 1.0, 2.0, 3.0, 4.0;
 * lat = 17.0, 23.0;
 * lon = -15.0, -1.0;
 * } testgrid1
 */

public class TestGrid1 extends TestSources
{
    static final protected String DATASET = "testgrid1.nc";

    static final protected String URLPATH_REMOTE =
            "/thredds/dodsC/testdods/"+DATASET;
    static final protected String URLPATH_LOCAL =
            "/thredds/dodsC/testdods/"+DATASET;

    protected String testserver = null;

    public TestGrid1()
    {
        setTitle("Simple DAP Grid");
        setSystemProperties();
    }

    @Test
    @Category(NeedsExternalResource.class)
    public void testGrid1()
            throws Exception
    {
        System.out.println("TestGrid1:");
        String url = null;
        boolean pass = true;
        NetcdfDataset ncfile = null;
        if(TestDir.threddsTestServer.startsWith("localhost"))
            url = "dods://" + TestDir.remoteTestServer + URLPATH_LOCAL;
        else
            url = "dods://" + TestDir.remoteTestServer + URLPATH_REMOTE;

        try {
            ncfile = NetcdfDataset.openDataset(url);
            pass = true;
        } catch (Exception e) {
            pass = false;
        }

        Assert.assertTrue("TestGrid1: cannot find dataset", pass);

        System.out.println("url: " + url);

        String metadata = null;
        String data = null;

        metadata = ncdumpmetadata(ncfile);

        if(prop_visual)
            visual(getTitle() + ".dds", metadata);
        if(true) {
            data = ncdumpdata(ncfile);
            if(prop_visual)
                visual(getTitle() + ".dods", data);

            if(prop_diff) { //compare with baseline
                // Read the baseline file(s)
                String diffs = compare("TestGrid1", "netcdf " + url + BASELINE,
                        data);
                if(diffs != null)
                    pass = false;
            }
        }
        Assert.assertTrue("Testing TestGrid1" + getTitle(), pass

        );
    }

    static protected final String BASELINE =
     " {\n"
    +"dimensions:\n"
    +"lat = 2;\n"
    +"lon = 2;\n"
    +"variables:\n"
    +"double var(lat=2, lon=2);\n"
    +":_CoordinateAxes = \"lat lon \";\n"
    +"float lat(lat=2);\n"
    +":_CoordinateAxisType = \"Lat\";\n"
    +"float lon(lon=2);\n"
    +":_CoordinateAxisType = \"Lon\";\n"
    +"// global attributes:\n"
    +":_CoordSysBuilder = \"ucar.nc2.dataset.conv.DefaultConvention\";\n"
    +"data:\n"
    +"var =\n"
    +"{\n"
    +"{0.0, 1.0},\n"
    +"{2.0, 3.0}\n"
    +"}\n"
    +"lat =\n"
    +"{17.0, 23.0}\n"
    +"lon =\n"
    +"{-15.0, -1.0}\n"
    +"}\n"
    ;
}
