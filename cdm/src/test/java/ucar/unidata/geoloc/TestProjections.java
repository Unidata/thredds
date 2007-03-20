package ucar.unidata.geoloc;
import junit.framework.*;

import ucar.unidata.geoloc.projection.*;
import java.awt.geom.Point2D;

/** test methods projections have in common
 *
 * @author John Caron
 * @version $Id: TestProjections.java 51 2006-07-12 17:13:13Z caron $
 */

public class TestProjections extends TestCase {
  int NTRIALS = 10000;
  double TOLERENCE = 1.0e-6;
  int count = 10;

  public TestProjections(String name ) {
    super(name);
  }

  boolean close (double d1, double d2) {
    if (count++ < 10)
      System.out.println(d1 +" " +d2);
    return Math.abs( d1-d2) < TOLERENCE;
  }

  void testProjection (ProjectionImpl proj) {
    java.util.Random r = new java.util.Random((long)this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    for (int i=0; i<NTRIALS; i++) {
      startL.setLatitude(180.0 * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(360.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj( startL);
      LatLonPoint endL = proj.projToLatLon( p);

      assert( close(startL.getLatitude(), endL.getLatitude()))  : proj.getClass().getName()+" failed start= "+startL+" end = "+ endL;
      assert( close(startL.getLongitude(), endL.getLongitude())) : proj.getClass().getName()+" failed start= "+startL+" end = "+ endL;
    }

    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i=0; i<NTRIALS; i++) {
      startP.setLocation(10000.0 * (r.nextDouble() - .5),  // random proj point
                      10000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon( startP);
      ProjectionPoint endP = proj.latLonToProj( ll);

      assert( close(startP.getX(), endP.getX()));
      assert( close(startP.getY(), endP.getY()));
    }

    System.out.println("Tested " +NTRIALS +" pts for projection " +proj.getClassName());
  }

  public void testLC() {
    testProjection(new LambertConformal());
  }

  public void testTM() {
    testProjection(new TransverseMercator());
  }

  public void testStereo() {
    testProjection(new Stereographic());
  }

  public void testLA() {
    testProjection(new LambertAzimuthalEqualArea());
  }

  public void utestOrtho() {
    testProjection(new Orthographic());
  }

  public void testAEA() {

    // LOOK failed on 8.697N 104.1E
    //  49.94N 108.5E
    // 16.29S 104.8E
    // 53.96S 77.06E
    // 23.00S 74.68E
    // start= 62.36N 82.76E end = 62.36N 144.1E
    testProjection(new AlbersEqualArea());

  }


}