package ucar.unidata.geoloc;

import ucar.unidata.geoloc.projection.*;
import junit.framework.*;

/**
 *
 * @author John Caron
 * @version $Id: TestLatLonProjection.java 51 2006-07-12 17:13:13Z caron $
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

}