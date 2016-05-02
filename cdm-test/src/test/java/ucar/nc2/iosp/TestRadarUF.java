package ucar.nc2.iosp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.Attribute;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests for the UF radar format
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestRadarUF {

    @Parameterized.Parameters(name="{0}")
    public static Collection metadata() {
        Object[][] data = new Object[][] {
                {"010820_2101-2107.uf", "EDOP/P1", "FLORIDAT", -85.02, 18.358,
                        19686.,
                        CalendarDate.of(null, 2001, 8, 20, 21, 1, 20),
                        CalendarDate.of(null, 2001, 8, 20, 21, 7, 32)},
                {"CHL20080702_225125_B.uf", "CSU-CHIL", "default", -104.637,
                        40.446, 1432.,
                        CalendarDate.of(null, 2008, 7, 2, 22, 52, 0),
                        CalendarDate.of(null, 2008, 7, 2, 22, 53, 42)},
                {"KTLX__sur_20080624.214247.uf", "NEXRAD", "NEXRAD", -97.277,
                        35.333, 384.,
                        CalendarDate.of(null, 2008, 6, 24, 21, 42, 47),
                        CalendarDate.of(null, 2008, 6, 24, 21, 42, 47)},
                {"NPOL_vol_010822_0000.uf", "ntr2", "ntr2", -81.675, 24.577, 2.,
                        CalendarDate.of(null, 2001, 8, 22, 0, 0, 23),
                        CalendarDate.of(null, 2001, 8, 22, 0, 5, 14)},
        };
        return Arrays.asList(data);
    }

    String fname, radarName, siteName;
    double lon, lat, height;
    CalendarDate start, end;

    public TestRadarUF(String fname, String radarName, String siteName,
                       double lon, double lat, double height,
                       CalendarDate start, CalendarDate end) {
        this.fname = fname;
        this.radarName = radarName;
        this.siteName = siteName;
        this.lon = lon;
        this.lat = lat;
        this.height = height;
        this.start = start;
        this.end = end;
    }

    @Test
    public void checkMetadata() throws IOException {
        String fileIn = TestDir.cdmUnitTestDir + "formats/uf/" + fname;
        try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
            Attribute att = ncf.findGlobalAttribute("StationLongitude");
            Assert.assertEquals(lon, att.getNumericValue().doubleValue(),
                    0.001);

            att = ncf.findGlobalAttribute("StationLatitude");
            Assert.assertEquals(lat, att.getNumericValue().doubleValue(),
                    0.001);

            att = ncf.findGlobalAttribute("StationElevationInMeters");
            Assert.assertEquals(height, att.getNumericValue().doubleValue(),
                    0.1);

            att = ncf.findGlobalAttribute("instrument_name");
            Assert.assertEquals(radarName, att.getStringValue());

            att = ncf.findGlobalAttribute("site_name");
            Assert.assertEquals(siteName, att.getStringValue());

            att = ncf.findGlobalAttribute("time_coverage_start");
            CalendarDate date = CalendarDate.parseISOformat(null,
                    att.getStringValue());
            Assert.assertEquals(start, date);

            att = ncf.findGlobalAttribute("time_coverage_end");
            date = CalendarDate.parseISOformat(null, att.getStringValue());
            Assert.assertEquals(end, date);
        }
    }
}
