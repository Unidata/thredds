package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPointImpl;

import java.lang.invoke.MethodHandles;

/**
 * Tests for {@link RotatedLatLon}.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class RotatedLatLonTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Tolerance for coordinate comparisons.
   */
  private static final double TOLERANCE = 1e-6;

  /**
   * A rotated lat/lon projection with origin at 54 degrees North, 254 degrees
   * East.
   */
  private Projection proj = new RotatedLatLon(-36, 254, 0);

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
    Assert.assertEquals("Unexpected longitude", 254 - 360, latlonResult.getLongitude(), TOLERANCE);
    Assert.assertEquals("Unexpected latitude", 54, latlonResult.getLatitude(), TOLERANCE);
  }

}
