package ucar.unidata.geoloc;

import junit.framework.*;

import ucar.unidata.geoloc.projection.*;
import ucar.nc2.TestAll;

import java.awt.geom.Point2D;

/**
 * test methods projections have in common
 *
 * @author John Caron
 * @version $Id: TestProjections.java 51 2006-07-12 17:13:13Z caron $
 */

public class TestProjections extends TestCase {
  boolean show = true;
  int NTRIALS = 10000;
  double TOLERENCE = 1.0e-6;
  int count = 10;

  public TestProjections(String name) {
    super(name);
  }

  private void doOne(ProjectionImpl proj, double lat, double lon) {
    LatLonPointImpl startL = new LatLonPointImpl(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPoint endL = proj.projToLatLon(p);

    System.out.println("start  = " + startL);
    System.out.println("end  = " + endL);

  }

  private void testProjection(ProjectionImpl proj) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    for (int i = 0; i < NTRIALS; i++) {
      startL.setLatitude(180.0 * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(360.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      assert (TestAll.closeEnough(startL.getLatitude(), endL.getLatitude(), 1.0e-4)) : proj.getClass().getName() + " failed start= " + startL + " end = " + endL;
      assert (TestAll.closeEnough(startL.getLongitude(), endL.getLongitude(), 1.0e-4)) : proj.getClass().getName() + " failed start= " + startL + " end = " + endL;
    }

    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i = 0; i < NTRIALS; i++) {
      startP.setLocation(10000.0 * (r.nextDouble() - .5),  // random proj point
              10000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon(startP);
      ProjectionPoint endP = proj.latLonToProj(ll);

      assert (TestAll.closeEnough(startP.getX(), endP.getX()));
      assert (TestAll.closeEnough(startP.getY(), endP.getY()));
    }

    System.out.println("Tested " + NTRIALS + " pts for projection " + proj.getClassName());
  }

  // must have lon within +/- lonMax
  public void testProjectionLonMax(ProjectionImpl proj, double lonMax, double latMax) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    for (int i = 0; i < NTRIALS; i++) {
      startL.setLatitude(latMax * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(lonMax * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      if (show) {
        System.out.println("start  = " + startL);
        System.out.println("end  = " + endL);
      }

      double tolerence = 5.0e-4;
      assert (TestAll.closeEnough(startL.getLatitude(), endL.getLatitude(), tolerence)) : proj.getClass().getName() + " failed start= " + startL + " end = " + endL;
      assert (TestAll.closeEnough(startL.getLongitude(), endL.getLongitude(), tolerence)) : proj.getClass().getName() + " failed start= " + startL + " end = " + endL;
    }

    startL.setLatitude(latMax / 2);
    startL.setLongitude(lonMax / 2);
    ProjectionPoint p = proj.latLonToProj(startL);
    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i = 0; i < NTRIALS; i++) {
      startP.setLocation(p.getX() * (r.nextDouble() - .5),  // random proj point
              p.getY() * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon(startP);
      ProjectionPoint endP = proj.latLonToProj(ll);

      assert (TestAll.closeEnough(startP.getX(), endP.getX())) : " failed start= " + startP.getX() + " end = " + endP.getX();
      assert (TestAll.closeEnough(startP.getY(), endP.getY())) : " failed start= " + startP.getY() + " end = " + endP.getY();
    }

    System.out.println("Tested " + NTRIALS + " pts for projection " + proj.getClassName());
  }

  public void testLC() {
    testProjection(new LambertConformal());

    LambertConformal lc = new LambertConformal();
    LambertConformal lc2 = (LambertConformal) lc.constructCopy();
    assert lc.equals(lc2);

  }

  public void testTM() {
    testProjection(new TransverseMercator());

    TransverseMercator p = new TransverseMercator();
    TransverseMercator p2 = (TransverseMercator) p.constructCopy();
    assert p.equals(p2);
  }

  public void testStereo() {
    testProjection(new Stereographic());
    Stereographic p = new Stereographic();
    Stereographic p2 = (Stereographic) p.constructCopy();
    assert p.equals(p2);
  }

  public void testLA() {
    testProjection(new LambertAzimuthalEqualArea());
    LambertAzimuthalEqualArea p = new LambertAzimuthalEqualArea();
    LambertAzimuthalEqualArea p2 = (LambertAzimuthalEqualArea) p.constructCopy();
    assert p.equals(p2);
  }

  public void testOrtho() {
    testProjectionLonMax(new Orthographic(), 180, 100);
    Orthographic p = new Orthographic();
    Orthographic p2 = (Orthographic) p.constructCopy();
    assert p.equals(p2);
  }

  public void testAEA() {
    testProjection(new AlbersEqualArea());
    AlbersEqualArea p = new AlbersEqualArea();
    AlbersEqualArea p2 = (AlbersEqualArea) p.constructCopy();
    assert p.equals(p2);
  }

  public void testFlatEarth() {
    testProjection(new FlatEarth());
    FlatEarth p = new FlatEarth();
    FlatEarth p2 = (FlatEarth) p.constructCopy();
    assert p.equals(p2);
  }

  public void testMercator() {
    testProjection(new Mercator());
    Mercator p = new Mercator();
    Mercator p2 = (Mercator) p.constructCopy();
    assert p.equals(p2);
  }

  // UTM failing - no not use
  public void utestUTM() {
    // 33.75N 15.25E end = 90.0N 143.4W
    //doOne(new UtmProjection(), 33.75, 15.25);

    testProjectionLonMax(new UtmProjection(), 100, 100);
    UtmProjection p = new UtmProjection();
    UtmProjection p2 = (UtmProjection) p.constructCopy();
    assert p.equals(p2);  // */
  }

  public void testVerticalPerspectiveView() {
    testProjectionLonMax(new VerticalPerspectiveView(), 100, 100);
    VerticalPerspectiveView p = new VerticalPerspectiveView();
    VerticalPerspectiveView p2 = (VerticalPerspectiveView) p.constructCopy();
    assert p.equals(p2);
  }


}