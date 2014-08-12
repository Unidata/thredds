package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.geoloc.*;

public class SinusoidalTest {
    // Reproduces issue from ETO-719860:
    // https://www.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=24221
    @Test
    public void testEto719860() {
        // The map area of the dataset. The upper-right corner is "off the earth", which is causing trouble when
        // converting from projection coords to lat/lon.
        double minX = 7783.190324950472;
        double maxX = 8895.140844616471;
        double minY = 6672.166430716527;
        double maxY = 7784.116950383528;

        Sinusoidal proj = new Sinusoidal();
        ProjectionRect projBB = new ProjectionRect(minX, minY, maxX, maxY);
        LatLonRect latLonBB = proj.projToLatLonBB(projBB);

        // Based on visual inspection in ToolsUI's Grid Viewer, these seem to be the correct lat/lon bounds.
        Assert.assertEquals(140.0, latLonBB.getLonMin(), 0.1);
        Assert.assertEquals(180.0, latLonBB.getLonMax(), 0.1);
        Assert.assertEquals(60.0,  latLonBB.getLatMin(), 0.1);
        Assert.assertEquals(67.11, latLonBB.getLatMax(), 0.1);
    }

    @Test
    public void testCalcMinAndMaxYs_1() {
        // Along the equator, in the default Sinusoidal, the range of valid x's is [-20015, +20015].
        double projX = 10_000;

        Sinusoidal proj = new Sinusoidal();
        double[] minAndMaxYs = proj.calcMinAndMaxYsAt(projX);

        ProjectionPoint projMinPoint = new ProjectionPointImpl(projX, minAndMaxYs[0]);
        ProjectionPoint projMaxPoint = new ProjectionPointImpl(projX, minAndMaxYs[1]);

        Assert.assertEquals(projX, projMinPoint.getX(), 1e-6);
        Assert.assertEquals(projX, projMaxPoint.getX(), 1e-6);

        LatLonPoint latLonMinPoint = proj.projToLatLon(projMinPoint);
        LatLonPoint latLonMaxPoint = proj.projToLatLon(projMaxPoint);

        Assert.assertEquals(180, latLonMinPoint.getLongitude(), 1e-6);
        Assert.assertEquals(180, latLonMaxPoint.getLongitude(), 1e-6);
    }

    @Test
    public void testCalcMinAndMaxYs_2() {
        // Along the equator, in the default Sinusoidal, the range of valid x's is [-20015, +20015].
        double projX = -4_545;

        Sinusoidal proj = new Sinusoidal();
        double[] minAndMaxYs = proj.calcMinAndMaxYsAt(projX);

        ProjectionPoint projMinPoint = new ProjectionPointImpl(projX, minAndMaxYs[0]);
        ProjectionPoint projMaxPoint = new ProjectionPointImpl(projX, minAndMaxYs[1]);

        Assert.assertEquals(projX, projMinPoint.getX(), 1e-6);
        Assert.assertEquals(projX, projMaxPoint.getX(), 1e-6);

        LatLonPoint latLonMinPoint = proj.projToLatLon(projMinPoint);
        LatLonPoint latLonMaxPoint = proj.projToLatLon(projMaxPoint);

        Assert.assertEquals(-180, latLonMinPoint.getLongitude(), 1e-6);
        Assert.assertEquals(-180, latLonMaxPoint.getLongitude(), 1e-6);
    }

    @Test
    public void testCalcMinAndMaxYs_3() {
        // Along the equator, in the default Sinusoidal, the range of valid x's is [-20015, +20015].
        double projX = 0;

        Sinusoidal proj = new Sinusoidal();
        double[] minAndMaxYs = proj.calcMinAndMaxYsAt(projX);

        ProjectionPoint projMinPoint = new ProjectionPointImpl(projX, minAndMaxYs[0]);
        ProjectionPoint projMaxPoint = new ProjectionPointImpl(projX, minAndMaxYs[1]);

        LatLonPoint latLonMinPoint = proj.projToLatLon(projMinPoint);
        LatLonPoint latLonMaxPoint = proj.projToLatLon(projMaxPoint);

        Assert.assertEquals(-90, latLonMinPoint.getLatitude(),  1e-6);
        Assert.assertEquals(0,   latLonMinPoint.getLongitude(), 1e-6);

        Assert.assertEquals(+90, latLonMaxPoint.getLatitude(),  1e-6);
        Assert.assertEquals(0,   latLonMaxPoint.getLongitude(), 1e-6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalcMinAndMaxYs_4() {
        // Along the equator, in the default Sinusoidal, the range of valid x's is [-20015, +20015].
        double projX = 30_000;

        Sinusoidal proj = new Sinusoidal();
        double[] minAndMaxYs = proj.calcMinAndMaxYsAt(projX);  // Should throw IllegalArgumentException.
    }
}
