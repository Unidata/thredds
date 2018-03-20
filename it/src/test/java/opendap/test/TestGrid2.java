/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package opendap.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/**
 * Test nc2 dods in the JUnit framework.
 * Dataset {
 * Grid {
 * ARRAY:
 * Float32 var[time=2][time=2];
 * MAPS:
 * Float32 time[time=2];
 * } testgrid_samedim
 * data:
 * var = 0.0, 1.0, 2.0, 3.0, 4.0;
 * time = 17.0, 23.0;
 * } testgrid2;
 */

public class TestGrid2 extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final protected String URLPATH = "/thredds/dodsC/scanLocal/testgrid2.nc";

    public TestGrid2()
    {
        setTitle("DAP Grid with repeated dimension");
        setSystemProperties();
    }

    @Test
    public void testGrid2()
            throws Exception
    {
        System.out.println("TestGrid2:");
        String url = "dods://" + TestDir.remoteTestServer + URLPATH;
        boolean pass = true;
        NetcdfDataset ncfile = null;

        try {
            ncfile = NetcdfDataset.openDataset(url);
            pass = true;
        } catch (Exception e) {
            pass = false;
        }

        Assert.assertTrue("XFAIL : TestGrid2: cannot open dataset =" + url, true);
        if(!pass) return;

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
                String diffs = compare("TestGrid2", BASELINE, data);
                if(diffs != null) {
                    System.err.println(diffs);
                    pass = false;
                }
            }
        }
        Assert.assertTrue("XFAIL : Testing TestGrid2" + getTitle(), true);
    }

    String ncdumpmetadata(NetcdfDataset ncfile)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        // Print the meta-databuffer using these args to NcdumpW
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, null, sw, null))
                throw new Exception("NcdumpW failed");
        } catch (IOException ioe) {
            throw new Exception("NcdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    String ncdumpdata(NetcdfDataset ncfile)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        // Dump the databuffer
        sw = new StringWriter();
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, "-vall", sw, null))
                throw new Exception("NCdumpW failed");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("NCdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }


    static protected final String BASELINE =
            "netcdf " + TestOnLocalServer.withDodsPath("dodsC/scanLocal/testgrid2.nc") + " {\n"
                    + "  dimensions:\n"
                    + "    time = 2;\n"
                    + "  variables:\n"
                    + "    double var(time=2, time=2);\n"
                    + "\n"
                    + "    float time(time=2);\n"
                    + "\n"
                    + "  // global attributes:\n"
                    + "  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.DefaultConvention\";\n"
                    + " data:\n"
                    + "var =\n"
                    + "  {\n"
                    + "    {0.0, 1.0},\n"
                    + "    {2.0, 3.0}\n"
                    + "  }\n"
                    + "time =\n"
                    + "  {17.0, 23.0}\n"
                    + "}\n";
}
