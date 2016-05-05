package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Test;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPointImpl;

/**
 * Tests for {@link RotatedPole}.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class RotatedPoleTest {

  /**
   * Tolerance for coordinate comparisons.
   */
  private static final double TOLERANCE = 1e-6;

  /**
   * A rotated lat/lon projection with origin at 54 degrees North, 254 degrees
   * East.
   */
  private Projection proj = new RotatedPole(90 - 54, LatLonPointImpl.lonNormal(254 + 180));

  /**
   * Test that the unrotated centre lat/lon is the origin of the rotated
   * projection.
   */
  @Test
  public void testLatLonToProj() {
    LatLonPointImpl latlon = new LatLonPointImpl(54, 254);
    ProjectionPointImpl result = new ProjectionPointImpl();
    proj.latLonToProj(latlon, result);
    Assert.assertEquals("Unexpected rotated longitude", 0, result.getX(), TOLERANCE);
    Assert.assertEquals("Unexpected rotated latitude", 0, result.getY(), TOLERANCE);
  }

  /**
   * Test that the origin of the rotated projection is the unrotated centre
   * lat/lon.
   */
  @Test
  public void testProjToLatLon() {
    ProjectionPointImpl p = new ProjectionPointImpl(0, 0);
    LatLonPointImpl latlonResult = new LatLonPointImpl();
    proj.projToLatLon(p, latlonResult);
    Assert.assertEquals("Unexpected longitude", LatLonPointImpl.lonNormal(254), latlonResult.getLongitude(), TOLERANCE);
    Assert.assertEquals("Unexpected latitude", 54, latlonResult.getLatitude(), TOLERANCE);
  }

}
