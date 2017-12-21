package ucar.unidata.geoloc.projection.sat;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class MSGnavigationTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Copied from MSGnavigation. I don't really care about their value, I just want them to be "reasonable".
    private static final double SAT_HEIGHT = 42164.0;     // distance from Earth centre to satellite
    private static final double R_EQ = 6378.169;   // radius from Earth centre to equator
    private static final double R_POL = 6356.5838;  // radius from Earth centre to pol

    // Demonstrates the bug described in TDS-575.
    @Test
    public void testConstructCopy() throws Exception {
        // These are the same as the values used in MSGnavigation() except for the 2nd, lon0. For the purposes of
        // this test, that's the only argument I care about.
        MSGnavigation msgNav =
                new MSGnavigation(0.0, 180, R_EQ, R_POL, SAT_HEIGHT, SAT_HEIGHT - R_EQ, SAT_HEIGHT - R_EQ);

        Assert.assertEquals(Math.PI, msgNav.getLon0(), 1e-6);  // 180° = π radians.

        // The 2 tests below failed prior to TDS-575 being fixed.

        MSGnavigation msgNavCopy1 = (MSGnavigation) msgNav.constructCopy();
        Assert.assertEquals(Math.PI, msgNavCopy1.getLon0(), 1e-6);  // 180° = π radians.

        MSGnavigation msgNavCopy2 = (MSGnavigation) msgNavCopy1.constructCopy();
        Assert.assertEquals(Math.PI, msgNavCopy2.getLon0(), 1e-6);  // 180° = π radians.
    }
}
