/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;
import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.Format;

import java.lang.invoke.MethodHandles;
import java.util.Random;

/** Test basic projection methods */

public class TestBasic extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final boolean debug1 = false;
  private final boolean debug2 = false;
  private final boolean debug3 = false;
  private final boolean debug4 = false;

  public TestBasic( String name) {
    super(name);
  }

  /////////////////// testLatLonArea /////////////////

  LatLonRect makeLatLonBoundingBox2(double lon1, double lon2) {
    LatLonPoint pt1 = new LatLonPointImpl(-10.0, lon1);
    LatLonPoint pt2 = new LatLonPointImpl(10.0, lon2);
    LatLonRect llbb = new LatLonRect(pt1, pt2);
    if (debug2) System.out.println( Format.formatDouble(lon1, 8, 5)+ " "+
      Format.formatDouble(lon2, 8, 5)+ " => "+ llbb+ " crossDateline= "+
      llbb.crossDateline());
    return llbb;
  }

  LatLonRect makeLatLonBoundingBox(double lon, double loninc) {
    LatLonPoint pt = new LatLonPointImpl(-10.0, lon);
    LatLonRect llbb = new LatLonRect(pt, 20.0, loninc);
    if (debug2) System.out.println( Format.formatDouble(lon, 8, 5)+ " "+
      Format.formatDouble(loninc, 8, 5)+ " => "+ llbb+ " crossDateline= "+
      llbb.crossDateline());
    return llbb;
  }

  void testContains(LatLonRect b1, LatLonRect b2) {
    if (debug3) System.out.println(b1+ " crossDateline= "+ b1.crossDateline());
    if (debug3) System.out.println(b2+ " crossDateline= "+ b2.crossDateline());
    if (debug3)
      b1.containedIn(b2);
    else
      assert( b1.containedIn(b2));
  }

  LatLonRect testExtend(LatLonRect b, LatLonPoint pt) {
    if (debug4) System.out.println("start "+b+ " crossDateline= "+ b.crossDateline());
    b.extend(pt);
    if (debug4) System.out.println("extend " +pt +" ==> " +b +" crossDateline= " +b.crossDateline());
    if (!debug4)
      assert(b.contains(pt));
    return b;
  }

  public void testGlobalBB() {
    Random rand = new Random(System.currentTimeMillis());
    int count = 0;
    while(count++ <1000) {
      double r = 360.*rand.nextFloat()-180;
      LatLonRect llbb = new LatLonRect(new LatLonPointImpl(20.0, r), 20.0, 360.0);
      double r2 = 360.*rand.nextFloat()-180;
      LatLonPointImpl p = new LatLonPointImpl(30.0, r2);
      assert llbb.contains(p);
    }
  }

  public void testLatLonBoundingBox() {
      /* check constructors */
    assert ( makeLatLonBoundingBox(140.0, 50.0).equals( makeLatLonBoundingBox(190.0, -50.0)));
    assert ( makeLatLonBoundingBox(140.0, -150.0).equals( makeLatLonBoundingBox(-10.0, 150.0)));
    assert ( makeLatLonBoundingBox(140.0, 50.0).equals( makeLatLonBoundingBox2( 140.0, 190.0)));
    assert ( makeLatLonBoundingBox(-170.0, 310.0).equals( makeLatLonBoundingBox2( 190.0, 140.0)));
    assert ( makeLatLonBoundingBox(190.0, 310.0).equals( makeLatLonBoundingBox2( 190.0, 140.0)));

      // check dateline crossings
    assert ( makeLatLonBoundingBox(140.0, 50.0).crossDateline());
    assert ( !makeLatLonBoundingBox(140.0, -150.0).crossDateline());
    assert ( makeLatLonBoundingBox(0.0, -250.0).crossDateline());
    assert ( !makeLatLonBoundingBox(-170.0, 300.0).crossDateline());
    assert ( makeLatLonBoundingBox2(140.0, 190.0).crossDateline());
    assert ( !makeLatLonBoundingBox2(190.0, 140.0).crossDateline());

      // degenerate cases: what to do with these?
    assert ( makeLatLonBoundingBox(-170.0, 370.0).crossDateline());

      // contains point
    LatLonPoint pt = new LatLonPointImpl(0.0, 177.0);
    assert( makeLatLonBoundingBox(140.0, 50.0).contains(pt));
    assert( !makeLatLonBoundingBox(190.0, 310.0).contains(pt));
    assert( makeLatLonBoundingBox2(140.0, 190.0).contains(pt));
    assert( !makeLatLonBoundingBox(190.0, 140.0).contains(pt));

      // contained in
    testContains( makeLatLonBoundingBox(140.0, 50.0), makeLatLonBoundingBox(140.0, 50.0));
    testContains( makeLatLonBoundingBox2(140.0, 50.0), makeLatLonBoundingBox2(140.0, 50.0));
    testContains( makeLatLonBoundingBox(140.0, 50.0), makeLatLonBoundingBox(0, 360.0));
    testContains( makeLatLonBoundingBox(300.0, 50.0), makeLatLonBoundingBox(0, 360.0));
    testContains( makeLatLonBoundingBox(50.0, 300.0), makeLatLonBoundingBox(0, 360.0));
    testContains( makeLatLonBoundingBox(50.0, 300.0), makeLatLonBoundingBox(-180.0, 360.0));
    testContains( makeLatLonBoundingBox2(190.0, 10.0), makeLatLonBoundingBox2(140.0, 50.0));
    testContains( makeLatLonBoundingBox(190.0, 10.0), makeLatLonBoundingBox(140.0, 60.0));

      // extend
    LatLonRect b;
    LatLonPoint p = new LatLonPointImpl(30.0, 30.0);
    b = testExtend(makeLatLonBoundingBox(10.0, 10.0), p);
    assert(p.nearlyEquals(b.getUpperRightPoint()));

    p = new LatLonPointImpl(-30.0, -30.0);
    b = testExtend(makeLatLonBoundingBox(10.0, 10.0), p);
    assert(p.nearlyEquals(b.getLowerLeftPoint()));

    p = new LatLonPointImpl(30.0, 190.0);
    b = testExtend(makeLatLonBoundingBox(50.0, 100.0), p);
    assert(p.nearlyEquals(b.getUpperRightPoint()));
    assert ( b.crossDateline());

    p = new LatLonPointImpl(-30.0, -50.0);
    b = testExtend(makeLatLonBoundingBox(50.0, 100.0), p);
    assert(p.nearlyEquals(b.getLowerLeftPoint()));
    assert ( !b.crossDateline());

    p = new LatLonPointImpl(-30.0, 100.0);
    b = testExtend(makeLatLonBoundingBox2(140.0, 50.0), p);
    assert(p.nearlyEquals(b.getLowerLeftPoint()));
    assert ( b.crossDateline());

    p = new LatLonPointImpl(30.0, 55.0);
    b = testExtend(makeLatLonBoundingBox2(140.0, 50.0), p);
    assert(p.nearlyEquals(b.getUpperRightPoint()));
    assert ( b.crossDateline());

  }


  //////////////////// testLatLonNormal ///////////////////////////

  void showLatLonNormal(double lon, double center) {
    System.out.println( Format.formatDouble(lon, 8, 5)+ " => "+
      Format.formatDouble(LatLonPointImpl.lonNormal( lon, center), 8, 5));
  }

  void runCenter(double center) {
    for (double lon=0.0; lon < 380.0; lon += 22.5) {
       if (debug1) showLatLonNormal( lon, center);
       double result = LatLonPointImpl.lonNormal( lon, center);
       assert( result >= center - 180.);
       assert( result <= center + 180.);
       assert( (result == lon) || (Math.abs( result-lon) == 360)
          || (Math.abs( result-lon) == 720));
    }
  }

  public void testLatLonNormal() {
    runCenter( 10.45454545454547);
    runCenter( 110.45454545454547);
    runCenter( 210.45454545454547);
    runCenter( -10.45454545454547);
    runCenter( -110.45454545454547);
    runCenter( -210.45454545454547);
    runCenter( 310.45454545454547);
  }

}
