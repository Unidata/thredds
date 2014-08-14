package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.geoloc.*;

public class SinusoidalTest {
    // If we want all of the x-coords in the geographic region to be positive, use this.
    // Will be roughly 20015.8.
    private static final double false_easting  = new Sinusoidal().latLonToProj(0, 180).getX();

    // If we want all of the y-coords in the geographic region to be positive, use this.
    // Will be roughly 10007.9.
    private static final double false_northing = new Sinusoidal().latLonToProj(90, 0).getY();

    // Reproduces issue from ETO-719860:
    // https://www.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=24221
    @Test
    public void testEto719860() {
        // The map area of the dataset. The upper-right corner is "off the earth", which is causing trouble when
        // converting from projection coords to lat/lon.
        double minX = 7783.190324950472;
        double minY = 6672.166430716527;
        double maxX = 8895.140844616471;
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
    public void projToLatLonBbValidBottom() {
        // Values come from visual inspection in ToolsUI->Grid Viewer
        // Lower left:  2889 3336  -> 30.004 N 30.011E
        // Lower right: 10055 3336 -> 30.004 N 104.42E
        // Upper left:  2889 9084  -> 81.697 N 179.98E
        ProjectionPoint lowerLeftProj  = new ProjectionPointImpl(2889, 3336);
        ProjectionPoint upperRightProj = new ProjectionPointImpl(10055, 10000);

        Sinusoidal proj = new Sinusoidal();
        ProjectionRect projBB = new ProjectionRect(lowerLeftProj, upperRightProj);
        LatLonRect latLonBB = proj.projToLatLonBB(projBB);

        Assert.assertEquals(30.011, latLonBB.getLonMin(), 0.1);
        Assert.assertEquals(180.0,  latLonBB.getLonMax(), 0.1);
        Assert.assertEquals(30.004, latLonBB.getLatMin(), 0.1);
        Assert.assertEquals(81.697, latLonBB.getLatMax(), 0.1);
    }

    @Test
    public void projToLatLonBbValidTop() {
        // Values come from visual inspection in ToolsUI->Grid Viewer
        // Upper left:  -9070 -2780 -> 25.002S 90.004W
        // Upper right: -3603 -2780 -> 25.002S 35.761W
        // Lower right: -3603 -8854 -> 79.627S 179.99W
        ProjectionPoint upperLeft  = new ProjectionPointImpl(-9070, -2780);
        ProjectionPoint lowerRight = new ProjectionPointImpl(-3603, -10000);

        Sinusoidal proj = new Sinusoidal();
        ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
        LatLonRect latLonBB = proj.projToLatLonBB(projBB);

        Assert.assertEquals(-180.0,  latLonBB.getLonMin(), 0.1);
        Assert.assertEquals(-35.761, latLonBB.getLonMax(), 0.1);
        Assert.assertEquals(-79.627, latLonBB.getLatMin(), 0.1);
        Assert.assertEquals(-25.002, latLonBB.getLatMax(), 0.1);
    }

    @Test
    public void projToLatLonBbValidLeft() {
        // Values come from visual inspection in ToolsUI->Grid Viewer
        // Upper left:  14480 -2228 -> 20.037S 138.62E
        // Lower left:  14480 -4361 -> 39.224S 168.10E
        // Upper right: 18803 -2228 -> 20.037S 179.99E
        ProjectionPoint upperLeft  = new ProjectionPointImpl(14480, -2228);
        ProjectionPoint lowerRight = new ProjectionPointImpl(20000, -4361);

        Sinusoidal proj = new Sinusoidal();
        ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
        LatLonRect latLonBB = proj.projToLatLonBB(projBB);

        Assert.assertEquals(138.62,  latLonBB.getLonMin(), 0.1);
        Assert.assertEquals(180.0 ,  latLonBB.getLonMax(), 0.1);
        Assert.assertEquals(-39.224, latLonBB.getLatMin(), 0.1);
        Assert.assertEquals(-20.037, latLonBB.getLatMax(), 0.1);
    }

    @Test
    public void projToLatLonBbValidRight() {
        // Values come from visual inspection in ToolsUI->Grid Viewer
        // Lower right: -9370 4446  -> 39.985N 109.99W
        // Upper right: -9370 6278  -> 56.465N 152.54W
        // Lower left:  -15334 4446 -> 39.985N 179.99W
        ProjectionPoint lowerRight = new ProjectionPointImpl(-9370,  4446);
        ProjectionPoint upperLeft  = new ProjectionPointImpl(-17500, 6278);

        Sinusoidal proj = new Sinusoidal();
        ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
        LatLonRect latLonBB = proj.projToLatLonBB(projBB);

        Assert.assertEquals(-180.0,  latLonBB.getLonMin(), 0.1);
        Assert.assertEquals(-109.99, latLonBB.getLonMax(), 0.1);
        Assert.assertEquals(39.985,  latLonBB.getLatMin(), 0.1);
        Assert.assertEquals(56.465,  latLonBB.getLatMax(), 0.1);
    }


    // Add test for when ll and lr straddle prime meridian. Max lat should be -+90.
    // Can we test that with Projection.crossSeam()? Why are some of the implementations so funky?

    // Add tests for the 4 cases of 1 good point.

    // Add test for a projBB that's completely off the earth.


    @Test
    public void testCalcMinAndMaxYs_1() {
        // Along the equator, in the default Sinusoidal, the range of valid x's is (-20016, +20016).
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
        double projX = false_easting - 4_545;

        Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, ProjectionImpl.EARTH_RADIUS);
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
        double projX = false_easting;

        Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, ProjectionImpl.EARTH_RADIUS);
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
        // Along the equator, in the default Sinusoidal, the range of valid x's is (-20016, +20016).
        double projX = 30_000;

        Sinusoidal proj = new Sinusoidal();
        proj.calcMinAndMaxYsAt(projX);  // Should throw IllegalArgumentException.
    }

    @Test
    public void testCalcMinAndMaxXs_1() {
        double projY = false_northing + 3_333;

        Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, ProjectionImpl.EARTH_RADIUS);
        double[] minAndMaxXs = proj.calcMinAndMaxXsAt(projY);

        ProjectionPoint projMinPoint = new ProjectionPointImpl(minAndMaxXs[0], projY);
        ProjectionPoint projMaxPoint = new ProjectionPointImpl(minAndMaxXs[1], projY);

        Assert.assertEquals(projY, projMinPoint.getY(), 1e-6);
        Assert.assertEquals(projY, projMaxPoint.getY(), 1e-6);

        LatLonPoint latLonMinPoint = proj.projToLatLon(projMinPoint);
        LatLonPoint latLonMaxPoint = proj.projToLatLon(projMaxPoint);

        Assert.assertEquals(-180, latLonMinPoint.getLongitude(), 1e-6);
        Assert.assertEquals(+180, latLonMaxPoint.getLongitude(), 1e-6);
    }

    @Test
    public void testCalcMinAndMaxXs_2() {
        double projY = false_northing;

        Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, ProjectionImpl.EARTH_RADIUS);
        double[] minAndMaxXs = proj.calcMinAndMaxXsAt(projY);

        ProjectionPoint projMinPoint = new ProjectionPointImpl(minAndMaxXs[0], projY);
        ProjectionPoint projMaxPoint = new ProjectionPointImpl(minAndMaxXs[1], projY);

        LatLonPoint latLonMinPoint = proj.projToLatLon(projMinPoint);
        LatLonPoint latLonMaxPoint = proj.projToLatLon(projMaxPoint);

        Assert.assertEquals(0,    latLonMinPoint.getLatitude(),  1e-6);
        Assert.assertEquals(-180, latLonMinPoint.getLongitude(), 1e-6);

        Assert.assertEquals(0,    latLonMaxPoint.getLatitude(),  1e-6);
        Assert.assertEquals(+180, latLonMaxPoint.getLongitude(), 1e-6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalcMinAndMaxXs_3() {
        // Along the prime meridian, in the default Sinusoidal, the range of valid y's is (-10008, +10008).
        double projY = -150_000;

        Sinusoidal proj = new Sinusoidal();
        proj.calcMinAndMaxXsAt(projY);  // Should throw IllegalArgumentException.
    }
}
