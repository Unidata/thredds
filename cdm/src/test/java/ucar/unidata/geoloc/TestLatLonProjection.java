/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.projection.*;
import junit.framework.*;

import java.lang.invoke.MethodHandles;

/**
 *
 * @author John Caron
 */
public class TestLatLonProjection extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private LatLonProjection p;

  public TestLatLonProjection(String name) {
    super(name);
  }

  public void setUp() {
    p = new LatLonProjection();
  }


  /*void showLatLonNormal(double lon, double center) {
    System.out.println( Format.formatDouble(lon, 8, 5)+ " => "+
      Format.formatDouble(LatLonPoint.lonNormal( lon, center), 8, 5));
  } */

  void runCenter() {
    LatLonPointImpl ptL = new LatLonPointImpl(-73.79, 0.0);
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      ptL.setLongitude(lon);
      LatLonRect llbb = new LatLonRect(ptL, yinc, xinc);

      ProjectionRect ma2 = p.latLonToProjBB(llbb);
      LatLonRect p2 = p.projToLatLonBB(ma2);

      Assert.assertTrue(llbb + " => " + ma2 + " => " + p2, llbb.nearlyEquals(p2));
    }
  }

  void runCenter(double center) {
    LatLonPointImpl ptL = new LatLonPointImpl(0.0, 0.0);
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      ptL.setLongitude(center+lon);
      LatLonRect llbb = new LatLonRect(ptL, yinc, xinc);

      ProjectionRect ma2 = p.latLonToProjBB(llbb);
      LatLonRect p2 = p.projToLatLonBB(ma2);

      Assert.assertTrue(llbb + " => " + ma2 + " => " + p2, llbb.nearlyEquals(p2));
    }
  }

  public void testLatLonToProjBB() {
    runCenter();
    runCenter( 110.45454545454547);
    runCenter( -110.45454545454547);
    runCenter( 0.0);
    runCenter( 420.0);
  }

  public LatLonRect testIntersection(LatLonRect bbox, LatLonRect bbox2) {
    LatLonRect result = bbox.intersect(bbox2);
    if (result != null)
      Assert.assertEquals("bbox= " + bbox.toString2() + "\nbbox2= " +
              bbox2.toString2() + "\nintersect= " +
              (result == null ? "null" : result.toString2()),
              bbox.intersect(bbox2), bbox2.intersect(bbox));
    return result;
  }

  public void testIntersection() {
    LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
    LatLonRect bbox2 = new LatLonRect(new LatLonPointImpl(-40.0, -180.0), 120.0, 300.0);
    assert testIntersection( bbox, bbox2) != null;

    bbox = new LatLonRect(new LatLonPointImpl(-90.0, -100.0), 90.0, 300.0);
    bbox2 = new LatLonRect(new LatLonPointImpl(-40.0, -180.0), 120.0, 300.0);
    assert testIntersection( bbox, bbox2) != null;

    bbox2 = new LatLonRect(new LatLonPointImpl(10, -180.0), 120.0, 300.0);
    assert testIntersection( bbox, bbox2) == null;

    bbox = new LatLonRect(new LatLonPointImpl(-90.0, -100.0), 90.0, 200.0);
    bbox2 = new LatLonRect(new LatLonPointImpl(-40.0, 120.0), 120.0, 300.0);
    assert testIntersection( bbox, bbox2) != null;

    bbox = new LatLonRect(new LatLonPointImpl(-90.0, -100.0), 90.0, 200.0);
    bbox2 = new LatLonRect(new LatLonPointImpl(-40.0, -220.0), 120.0, 140.0);
    assert testIntersection( bbox, bbox2) != null;
  }

  private LatLonRect testExtend(LatLonRect bbox, LatLonRect bbox2) {
    bbox.extend( bbox2);
    return bbox;
  }

  public void testExtend() {
    LatLonRect bbox;

    //     ---------
    //   --------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, 30.0), new LatLonPointImpl(-60.0, 120.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, -10.0), new LatLonPointImpl(-60.0, 55.0)));
    Assert.assertEquals(bbox.toString2(), 130.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    //   ---------
    //     ----
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -200.0), new LatLonPointImpl(-60.0, -100.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 177.0), new LatLonPointImpl(-60.0, 200.0)));
    Assert.assertEquals(bbox.toString2(), 100.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    //  ---------
    //     --------------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -200.0), new LatLonPointImpl(-60.0, -100.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, -150.0), new LatLonPointImpl(-60.0, 200.0)));
    Assert.assertEquals(bbox.toString2(), 360.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    //  -------
    //         ---------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 135.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 180.0)));
    Assert.assertEquals(bbox.toString2(), 360.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    //  ------
    //            ------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 0.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 160.0)));
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    //  ---------
    //               ------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 0.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 180.0)));
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    //         ---------
    //   ------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 180.0)),
        new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 0.0)));
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());
  }

}
