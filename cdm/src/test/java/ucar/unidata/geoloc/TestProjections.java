/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc;

import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;
import ucar.unidata.geoloc.projection.proj4.EquidistantAzimuthalProjection;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;

/**
 * test methods projections have in common
 *
 * @author John Caron
 */

public class TestProjections {
  boolean show = false;
  int NTRIALS = 10000;
  double tolerence = 5.0e-4;

  private LatLonPoint doOne(ProjectionImpl proj, double lat, double lon, boolean show) {
    LatLonPointImpl startL = new LatLonPointImpl(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    if (Double.isNaN(p.getX()) || Double.isNaN(p.getY())) return LatLonPointImmutable.INVALID;
    if (Double.isInfinite(p.getX()) || Double.isInfinite(p.getY())) return LatLonPointImmutable.INVALID;
    LatLonPointImpl endL = (LatLonPointImpl) proj.projToLatLon(p);

    if (show) {
      System.out.println("start  = " + startL.toString(8));
      System.out.println("projection point  = " + p.toString());
      System.out.println("end  = " + endL.toString(8));
    }
    return endL;
  }

  @Test
  //     java.lang.AssertionError: .072111263S 165.00490E expected:<-0.07211126381547306> but was:<39.99999999999999>
  public void testTMproblem() {
    double lat = -.072111263;
    double lon = 165.00490;
    LatLonPoint endL = doOne( new TransverseMercator(), lat, lon, true);
    if (endL.equals(LatLonPointImmutable.INVALID)) return;
    Assert.assertEquals(lat, endL.getLatitude(), tolerence);
    Assert.assertEquals(lon, endL.getLongitude(), tolerence);

  }

  private void testProjection(ProjectionImpl proj) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    int countT1 = 0;
    for (int i = 0; i < NTRIALS; i++) {
      startL.setLatitude(180.0 * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(360.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj(startL);
      if (Double.isNaN(p.getX()) || Double.isNaN(p.getY())) continue;
      LatLonPoint endL = proj.projToLatLon(p);
      if (Double.isNaN(endL.getLatitude()) ||
              Double.isNaN(endL.getLongitude()) ||
              endL.equals(LatLonPointImmutable.INVALID)) continue;

      Assert.assertEquals(startL.toString(8), startL.getLatitude(), endL.getLatitude(), 1.0e-3);
      Assert.assertEquals(startL.toString(8), startL.getLongitude(), endL.getLongitude(), 1.0e-3);
      countT1++;
    }

    int countT2 = 0;
    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i = 0; i < NTRIALS; i++) {
      startP.setLocation(10000.0 * (r.nextDouble() - .5),  // random proj point
          10000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon(startP);
      if (Double.isNaN(ll.getLatitude()) || Double.isNaN(ll.getLongitude())) continue;
      ProjectionPoint endP = proj.latLonToProj(ll);
      if (Double.isNaN(endP.getX()) || Double.isNaN(endP.getY())) continue;

      Assert.assertEquals(startP.toString(), startP.getX(), endP.getX(), tolerence);
      Assert.assertEquals(startP.toString(), startP.getY(), endP.getY(), tolerence);
      countT2++;
    }
    if (show)
      System.out.printf("Tested %d, %d pts for projection %s %n", countT1,
              countT2, proj.getClassName());
  }

  // must have lon within +/- lonMax, lat within +/- latMax
  public void testProjectionLonMax(ProjectionImpl proj, double lonMax, double latMax) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    double minx = Double.MAX_VALUE;
    double maxx = -Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxy = -Double.MAX_VALUE;
    for (int i = 0; i < NTRIALS; i++) {
      startL.setLatitude(latMax * (2 * r.nextDouble() - 1)); // random latlon point
      startL.setLongitude(lonMax * (2 * r.nextDouble() - 1));

      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      if (show) {
        System.out.println("startL  = " + startL);
        System.out.println("inter  = " + p);
        System.out.println("endL  = " + endL);
      }

      Assert.assertEquals(startL.toString(8), startL.getLatitude(), endL.getLatitude(), tolerence);
      Assert.assertEquals(startL.toString(8), startL.getLongitude(), endL.getLongitude(), tolerence);

      minx = Math.min(minx, p.getX());
      maxx = Math.max(maxx, p.getX());
      miny = Math.min(miny, p.getY());
      maxy = Math.max(maxy, p.getY());
    }

    double rangex = maxx - minx;
    double rangey = maxy - miny;
    if (show) {
      System.out.printf("rangex  = (%f,%f) %n", minx, maxx);
      System.out.printf("rangey  = (%f,%f) %n", miny, maxy);
    }

    startL.setLatitude(latMax / 2);
    startL.setLongitude(lonMax / 2);
    ProjectionPointImpl base = new ProjectionPointImpl();
    proj.latLonToProj(startL, base);
    ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i = 0; i < NTRIALS; i++) {
      double x = minx + rangex * r.nextDouble();
      double y = miny + rangey * r.nextDouble();
      startP.setLocation(x, y);

      try {
        LatLonPoint ll = proj.projToLatLon(startP);
        ProjectionPoint endP = proj.latLonToProj(ll);


        if (show) {
          System.out.println("start  = " + startP);
          System.out.println("interL  = " + ll);
          System.out.println("end  = " + endP);
        }

        Assert.assertEquals(startP.toString(), startP.getX(), endP.getX(), tolerence);
        Assert.assertEquals(startP.toString(), startP.getY(), endP.getY(), tolerence);
      } catch (IllegalArgumentException e) {
        System.out.printf("IllegalArgumentException=%s%n", e.getMessage());
        continue;
      }
    }

    if (show)
      System.out.println("Tested " + NTRIALS + " pts for projection " + proj
              .getClassName());
  }

  // must have x within +/- xMax, y within +/- yMax
  public void testProjectionProjMax(ProjectionImpl proj, double xMax,
                                    double yMax) {
      java.util.Random r = new java.util.Random((long) this.hashCode());
      ProjectionPointImpl startP = new ProjectionPointImpl();
      for (int i = 0; i < NTRIALS; i++) {
          double x = xMax * (2 * r.nextDouble() - 1);
          double y = yMax * (2 * r.nextDouble() - 1);
          startP.setLocation(x, y);
          try {
              LatLonPoint ll = proj.projToLatLon(startP);
              ProjectionPoint endP = proj.latLonToProj(ll);
              if (show) {
                  System.out.println("start  = " + startP);
                  System.out.println("interL  = " + ll);
                  System.out.println("end  = " + endP);
              }

            Assert.assertEquals(startP.toString(), startP.getX(), endP.getX(), tolerence);
            Assert.assertEquals(startP.toString(), startP.getY(), endP.getY(), tolerence);
          } catch (IllegalArgumentException e) {
              System.out.printf("IllegalArgumentException=%s%n", e.getMessage());
              continue;
          }
      }
      if (show) System.out.println("Tested " + NTRIALS + " pts for projection " + proj.getClassName());
  }

  @Test
  public void testLC() {
    testProjection(new LambertConformal());
    LambertConformal lc = new LambertConformal();
    LambertConformal lc2 = (LambertConformal) lc.constructCopy();
    assert lc.equals(lc2);
  }

  @Test
  public void testLCseam() {
    // test seam crossing
    LambertConformal lc = new LambertConformal(40.0, 180.0, 20.0, 60.0);
    ProjectionPointImpl p1 = (ProjectionPointImpl) lc.latLonToProj(new LatLonPointImpl(0.0, -1.0), new ProjectionPointImpl());
    ProjectionPointImpl p2 = (ProjectionPointImpl) lc.latLonToProj(new LatLonPointImpl(0.0, 1.0), new ProjectionPointImpl());
    if (show) {
      System.out.printf(" p1= x=%f y=%f%n", p1.getX(), p1.getY());
      System.out.printf(" p2= x=%f y=%f%n", p2.getX(), p2.getY());
    }
    assert lc.crossSeam(p1, p2);
  }

  @Test
  public void testTM() {
    testProjection(new TransverseMercator());

    TransverseMercator p = new TransverseMercator();
    TransverseMercator p2 = (TransverseMercator) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testStereo() {
    testProjection(new Stereographic());
    Stereographic p = new Stereographic();
    Stereographic p2 = (Stereographic) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testLA() {
    testProjection(new LambertAzimuthalEqualArea());
    LambertAzimuthalEqualArea p = new LambertAzimuthalEqualArea();
    LambertAzimuthalEqualArea p2 = (LambertAzimuthalEqualArea) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testOrtho() {
    testProjectionLonMax(new Orthographic(), 10, 10);
    Orthographic p = new Orthographic();
    Orthographic p2 = (Orthographic) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testAEA() {
    testProjection(new AlbersEqualArea());
    AlbersEqualArea p = new AlbersEqualArea();
    AlbersEqualArea p2 = (AlbersEqualArea) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testCEA() {
    testProjection(new CylindricalEqualAreaProjection());
    CylindricalEqualAreaProjection p = new CylindricalEqualAreaProjection();
    CylindricalEqualAreaProjection p2 = (CylindricalEqualAreaProjection) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testEAP() {
    testProjection(new EquidistantAzimuthalProjection());
    EquidistantAzimuthalProjection p = new EquidistantAzimuthalProjection();
    EquidistantAzimuthalProjection p2 = (EquidistantAzimuthalProjection) p.constructCopy();
    assert p.equals(p2);
  }

  public void utestAEAE() {
    testProjectionLonMax(new AlbersEqualAreaEllipse(), 180, 80);
    AlbersEqualAreaEllipse p = new AlbersEqualAreaEllipse();
    AlbersEqualAreaEllipse p2 = (AlbersEqualAreaEllipse) p.constructCopy();
    assert p.equals(p2);
  }

  public void utestLCCE() {
    testProjectionLonMax(new LambertConformalConicEllipse(), 360, 80);
    LambertConformalConicEllipse p = new LambertConformalConicEllipse();
    LambertConformalConicEllipse p2 = (LambertConformalConicEllipse) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testFlatEarth() {
    testProjectionProjMax(new FlatEarth(), 5000, 5000);
    FlatEarth p = new FlatEarth();
    FlatEarth p2 = (FlatEarth) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testMercator() {
    testProjection(new Mercator());
    Mercator p = new Mercator();
    Mercator p2 = (Mercator) p.constructCopy();
    assert p.equals(p2);
  }

  private void showProjVal(ProjectionImpl proj, double lat, double lon) {
    LatLonPointImpl startL = new LatLonPointImpl(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    if (show)
      System.out.printf("lat,lon= (%f, %f) x, y= (%f, %f) %n", lat, lon,
               p.getX(), p.getY());
  }

  @Test
  public void testMSG() {
    doOne(new MSGnavigation(), 60, 60, true);
    testProjection(new MSGnavigation());

    MSGnavigation m = new MSGnavigation();
    showProjVal(m, 0, 0);
    showProjVal(m, 60, 0);
    showProjVal(m, -60, 0);
    showProjVal(m, 0, 60);
    showProjVal(m, 0, -60);
   }

  @Test
  public void testRotatedPole() {
    testProjectionLonMax(new RotatedPole(37, 177), 360, 88);
    RotatedPole p = new RotatedPole();
    RotatedPole p2 = (RotatedPole) p.constructCopy();
    assert p.equals(p2);
  }

  /*
     grid_south_pole_latitude = -30.000001907348633
    grid_south_pole_longitude = -15.000000953674316
    grid_south_pole_angle = 0.0 */
  @Test
  public void testRotatedLatLon() {
    //   public RotatedLatLon(double southPoleLat, double southPoleLon, double southPoleAngle) {

    testProjectionLonMax(new RotatedLatLon(-30, -15, 0), 360, 88);
    RotatedLatLon p = new RotatedLatLon();
    RotatedLatLon p2 = (RotatedLatLon) p.constructCopy();
    assert p.equals(p2);
  }

  @Test
  public void testSinusoidal() {
    doOne(new Sinusoidal(0, 0, 0, 6371.007), 20, 40, true);
    testProjection(new Sinusoidal(0, 0, 0, 6371.007));
    Sinusoidal p = new Sinusoidal();
    Sinusoidal p2 = (Sinusoidal) p.constructCopy();
    assert p.equals(p2);
  }

  // UTM failing - no not use
  @Test
  public void testUTM() {
    // The central meridian = (zone * 6 - 183) degrees, where zone in [1,60].
    // zone = (lon + 183)/6
    // 33.75N 15.25E end = 90.0N 143.4W
    // doOne(new UtmProjection(10, true), 33.75, -122);
    testProjectionUTM(-12.89, .07996);

    testProjectionUTM(NTRIALS);

    UtmProjection p = new UtmProjection();
    UtmProjection p2 = (UtmProjection) p.constructCopy();
    assert p.equals(p2);  // */
  }

  private void testProjectionUTM(double lat, double lon) {
    LatLonPointImpl startL = new LatLonPointImpl();

    startL.setLatitude(lat);
    startL.setLongitude(lon);
    int zone = (int) ((lon + 183) / 6);
    UtmProjection proj = new UtmProjection(zone, lat >= 0.0);

    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPoint endL = proj.projToLatLon(p);

    if (show) {
      System.out.println("startL  = " + startL);
      System.out.println("inter  = " + p);
      System.out.println("endL  = " + endL);
    }

    Assert.assertEquals(startL.toString(8), startL.getLatitude(), endL.getLatitude(), 1.3e-4);
    Assert.assertEquals(startL.toString(8), startL.getLongitude(), endL.getLongitude(), 1.3e-4);
  }


  private void testProjectionUTM(int n) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    LatLonPointImpl startL = new LatLonPointImpl();

    for (int i = 0; i < n; i++) {
      startL.setLatitude(180.0 * (r.nextDouble() - .5)); // random latlon point
      startL.setLongitude(360.0 * (r.nextDouble() - .5));

      double lat = startL.getLatitude();
      double lon = startL.getLongitude();
      int zone = (int) ((lon + 183) / 6);
      UtmProjection proj = new UtmProjection(zone, lat >= 0.0);

      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      if (show) {
        System.out.println("startL  = " + startL);
        System.out.println("inter  = " + p);
        System.out.println("endL  = " + endL);
      }

      Assert.assertEquals(startL.toString(8), startL.getLatitude(), endL.getLatitude(), 1.0e-4);
      Assert.assertEquals(startL.toString(8), startL.getLongitude(), endL.getLongitude(), .02);
    }

    /* ProjectionPointImpl startP = new ProjectionPointImpl();
    for (int i = 0; i < NTRIALS; i++) {
      startP.setLocation(10000.0 * (r.nextDouble() - .5),  // random proj point
              10000.0 * (r.nextDouble() - .5));

      double lon =  startL.getLongitude();
      int zone = (int) ((lon + 183)/6);
      UtmProjection proj = new UtmProjection(zone, lon >= 0.0);

      LatLonPoint ll = proj.projToLatLon(startP);
      ProjectionPoint endP = proj.latLonToProj(ll);

      assert (TestAll.closeEnough(startP.getX(), endP.getX()));
      assert (TestAll.closeEnough(startP.getY(), endP.getY()));
    }  */

    if (show)
      System.out.println("Tested " + n + " pts for UTM projection ");
  }

  public void utestVerticalPerspectiveView() {
    testProjectionLonMax(new VerticalPerspectiveView(), 66, 60);
    VerticalPerspectiveView p = new VerticalPerspectiveView();
    VerticalPerspectiveView p2 = (VerticalPerspectiveView) p.constructCopy();
    assert p.equals(p2);
  }


}