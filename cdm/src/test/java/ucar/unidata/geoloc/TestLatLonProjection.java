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

import ucar.unidata.geoloc.projection.*;
import junit.framework.*;

/**
 *
 * @author John Caron
 */
public class TestLatLonProjection extends TestCase {

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

      assert llbb.equals( p2) : llbb + " => " + ma2 + " => " + p2;

      System.out.println(llbb + " => " + p2);
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

      assert llbb.equals( p2) : llbb + " => " + ma2 + " => " + p2;

      System.out.println(llbb + " => " + p2);
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
    System.out.println("\n     bbox= "+ bbox.toString2());
    System.out.println("    bbox2= "+bbox2.toString2());
    LatLonRect result = bbox.intersect(bbox2);
    System.out.println("intersect= "+(result == null ? "null" : result.toString2()));
    //System.out.println("intersect= "+bbox2.intersect(bbox).toString2());
    if (result != null)
      assert bbox.intersect(bbox2).equals(bbox2.intersect(bbox));
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
    System.out.println("\n bbox ="+bbox.toString2()+" width="+bbox.getWidth());
    System.out.println(" bbox extend by="+bbox2.toString2()+" width="+bbox2.getWidth());
    bbox.extend( bbox2);
    System.out.println(" result="+bbox.toString2()+" width="+bbox.getWidth());
    return bbox;
  }

  public void testExtend() {
    LatLonRect bbox;

    //     ---------
    //   --------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, 30.0), new LatLonPointImpl(-60.0, 120.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, -10.0), new LatLonPointImpl(-60.0, 55.0)));
    assert bbox.getWidth() == 130.0;
    assert !bbox.crossDateline();

    //   ---------
    //     ----
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -200.0), new LatLonPointImpl(-60.0, -100.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 177.0), new LatLonPointImpl(-60.0, 200.0)));
    assert bbox.getWidth() == 100.0;
    assert bbox.crossDateline();

    //  ---------
    //     --------------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -200.0), new LatLonPointImpl(-60.0, -100.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, -150.0), new LatLonPointImpl(-60.0, 200.0)));
    assert bbox.getWidth() == 360.0;
    assert !bbox.crossDateline();

    //  -------
    //         ---------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 135.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 180.0)));
    assert bbox.getWidth() == 360.0;
    assert !bbox.crossDateline();

    //  ------
    //            ------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 0.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 160.0)));
    assert bbox.getWidth() == 225.0;
    assert bbox.crossDateline();

    //  ---------
    //               ------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 0.0)),
                        new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 180.0)));
    assert bbox.getWidth() == 225.0;
    assert bbox.crossDateline();

    //         ---------
    //   ------
    bbox = testExtend( new LatLonRect(new LatLonPointImpl(-81.0, 135.0), new LatLonPointImpl(-60.0, 180.0)),
        new LatLonRect(new LatLonPointImpl(-81.0, -180.0), new LatLonPointImpl(-60.0, 0.0)));
    assert bbox.getWidth() == 225.0;
    assert bbox.crossDateline();
  }


}