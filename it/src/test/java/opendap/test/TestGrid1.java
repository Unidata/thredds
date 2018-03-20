/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package opendap.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

import java.lang.invoke.MethodHandles;

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

public class TestGrid1 extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final protected String URLPATH = "/thredds/dodsC/scanLocal/testgrid1.nc";

    public TestGrid1()
    {
        setTitle("Simple DAP Grid");
        setSystemProperties();
    }

    @Test
    public void testGrid1()
            throws Exception
    {
        System.out.println("TestGrid1:");
        String url = "dods://" + TestDir.remoteTestServer + URLPATH;
        boolean pass = true;
        NetcdfDataset ncfile = null;

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

        metadata = ncdumpmetadata(ncfile,null);

        if(prop_visual)
            visual(getTitle() + ".dds", metadata);
        if(true) {
            data = ncdumpdata(ncfile,null);
            if(prop_visual)
                visual(getTitle() + ".dods", data);

            if(prop_diff) { //compare with baseline
                // Compare to the baseline file(s)
                String ncurl = NetcdfFile.makeValidCDLName(url);
                // strip trailing .nc
                if(ncurl.endsWith(".nc"))
                    ncurl = ncurl.substring(0,ncurl.length()-3);
                String diffs = compare("TestGrid1", "netcdf " + ncurl + BASELINE,
                        data);
                if(diffs != null)
                    pass = false;
                System.err.println(diffs);
            }
        }Assert.assertTrue("Testing TestGrid1" + getTitle(), pass

        );
    }

    static protected final String BASELINE =
            " {\n"
                    +"dimensions:\n"
                    +"lat = 2;\n"
                    +"lon = 2;\n"
                    +"variables:\n"
                    +"double var(lat, lon);\n"
                    +"String var:_CoordinateAxes = \"lat lon \";\n"
                    +"float lat(lat);\n"
                    +"String lat:_CoordinateAxisType = \"Lat\";\n"
                    +"float lon(lon);\n"
                    +"String lon:_CoordinateAxisType = \"Lon\";\n"
                    +"// global attributes:\n"
                    +"String :_CoordSysBuilder = \"ucar.nc2.dataset.conv.DefaultConvention\";\n"
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
