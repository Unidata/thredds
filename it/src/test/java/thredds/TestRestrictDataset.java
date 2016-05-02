/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test restricted datasets
 *
 * @author caron
 * @since 3/16/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestRestrictDataset
{
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getTestParameters()
    {
        return Arrays.asList(new Object[][]{
            {"/dodsC/testRestrictedDataset/testData2.nc.dds"},
            {"/cdmremote/testRestrictedDataset/testData2.nc?req=form"},
            {"/fileServer/testRestrictedDataset/testData2.nc"},
            {"/wms/testRestrictedDataset/testData2.nc?service=WMS&version=1.3.0&request=GetCapabilities"},

            // restricted DatasetScan
            {"/dodsC/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc.html"},
            {"/cdmremote/testRestrictedScan/GFS_CONUS_80km_20120229_1200.grib1?req=form"},
            {"/fileServer/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc"},
            {"/wms/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc?service=WMS&version=1.3.0&request=GetCapabilities"},

            // restricted GRIB collections
            {"/dodsC/restrictCollection/GFS_CONUS_80km/TwoD.dds"},
            {"/ncss/restrictCollection/GFS_CONUS_80km/TwoD/dataset.html"},
            {"/cdmremote/restrictCollection/GFS_CONUS_80km/TwoD?req=form"},
        });
    }

    String path, query;

    public TestRestrictDataset(String path)
    {
        this.path = path;
    }


    @Test
    public void testRestriction()
    {
        //String server = "http://"+TestDir.threddsTestServer+"/thredds/";
        //String endpoint = server + path;

        String endpoint = TestWithLocalServer.withPath(path);
        System.out.printf("testRestriction req = '%s'%n", endpoint);
        try {
            try (HTTPMethod method = HTTPFactory.Get(endpoint)) {
                int statusCode = method.execute();
                if(statusCode != 401 && statusCode != 403)
                    assert false;
                // Assert.assertEquals(401, statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}

