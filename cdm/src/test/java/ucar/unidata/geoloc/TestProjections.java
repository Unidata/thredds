package ucar.unidata.geoloc;
import junit.framework.*;

import ucar.unidata.geoloc.projection.*;
import ucar.nc2.TestAll;

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

  /* boolean close (double d1, double d2) {
    if (count++ < 10)
      System.out.println(d1 +" " +d2);
    return Math.abs( d1-d2) < TOLERENCE;
  } */

  private void testProjection (ProjectionImpl proj) {
    java.util.Random r = new java.util.Random((long)this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    for (int i=0; i<NTRIALS; i++) {
      startL.setLatitude(180.0 * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(360.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj( startL);
      LatLonPoint endL = proj.projToLatLon( p);

      assert( TestAll.closeEnough(startL.getLatitude(), endL.getLatitude()))  : proj.getClass().getName()+" failed start= "+startL+" end = "+ endL;
      assert( TestAll.closeEnough(startL.getLongitude(), endL.getLongitude())) : proj.getClass().getName()+" failed start= "+startL+" end = "+ endL;
    }

    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i=0; i<NTRIALS; i++) {
      startP.setLocation(10000.0 * (r.nextDouble() - .5),  // random proj point
                      10000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon( startP);
      ProjectionPoint endP = proj.latLonToProj( ll);

      assert( TestAll.closeEnough(startP.getX(), endP.getX()));
      assert( TestAll.closeEnough(startP.getY(), endP.getY()));
    }

    System.out.println("Tested " +NTRIALS +" pts for projection " +proj.getClassName());
  }

  // must have lon within 180, x,y within Earth Radius
  public void testProjectionLon180 (ProjectionImpl proj) {
    java.util.Random r = new java.util.Random((long)this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    for (int i=0; i<NTRIALS; i++) {
      startL.setLatitude(180.0 * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(180.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj( startL);
      LatLonPoint endL = proj.projToLatLon( p);

      double tolerence = 5.0e-4;
      assert( TestAll.closeEnough(startL.getLatitude(), endL.getLatitude(), tolerence))  : proj.getClass().getName()+" failed start= "+startL+" end = "+ endL;
      assert( TestAll.closeEnough(startL.getLongitude(), endL.getLongitude(), tolerence)) : proj.getClass().getName()+" failed start= "+startL+" end = "+ endL;
    }

    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i=0; i<NTRIALS; i++) {
      startP.setLocation(5000.0 * (r.nextDouble() - .5),  // random proj point
                      5000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon( startP);
      ProjectionPoint endP = proj.latLonToProj( ll);

      assert( TestAll.closeEnough(startP.getX(), endP.getX())) : " failed start= "+startP.getX()+" end = "+ endP.getX();
      assert( TestAll.closeEnough(startP.getY(), endP.getY())) : " failed start= "+startP.getY()+" end = "+ endP.getY();
    }

    System.out.println("Tested " +NTRIALS +" pts for projection " +proj.getClassName());
  }

  public void testLC() {
    testProjection(new LambertConformal());

    LambertConformal lc = new LambertConformal();
    LambertConformal lc2 = (LambertConformal) lc.clone();
    assert lc.equals(lc2);

  }

  public void testTM() {
    testProjection(new TransverseMercator());

    TransverseMercator p = new TransverseMercator();
    TransverseMercator p2 = (TransverseMercator) p.clone();
    assert p.equals(p2);
  }

  public void testStereo() {
    testProjection(new Stereographic());
    Stereographic p = new Stereographic();
    Stereographic p2 = (Stereographic) p.clone();
    assert p.equals(p2);
  }

  public void testLA() {
    testProjection(new LambertAzimuthalEqualArea());
    LambertAzimuthalEqualArea p = new LambertAzimuthalEqualArea();
    LambertAzimuthalEqualArea p2 = (LambertAzimuthalEqualArea) p.clone();
    assert p.equals(p2);
  }

  public void testOrtho() {
    testProjectionLon180(new Orthographic());
    Orthographic p = new Orthographic();
    Orthographic p2 = (Orthographic) p.clone();
    assert p.equals(p2);
  }

  public void testAEA() {
     testProjection(new AlbersEqualArea());
     AlbersEqualArea p = new AlbersEqualArea();
     AlbersEqualArea p2 = (AlbersEqualArea) p.clone();
     assert p.equals(p2);
   }

  public void testFlatEarth() {
     testProjection(new FlatEarth());
     FlatEarth p = new FlatEarth();
     FlatEarth p2 = (FlatEarth) p.clone();
     assert p.equals(p2);
   }

  public void testMercator() {
      testProjection(new Mercator());
      Mercator p = new Mercator();
      Mercator p2 = (Mercator) p.clone();
      assert p.equals(p2);
    }

  public void testUTM() {
      testProjection(new UtmProjection());
      UtmProjection p = new UtmProjection();
      UtmProjection p2 = (UtmProjection) p.clone();
      assert p.equals(p2);
    }


}