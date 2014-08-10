package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

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
}
