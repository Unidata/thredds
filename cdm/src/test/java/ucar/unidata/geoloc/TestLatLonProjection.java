package ucar.unidata.geoloc;

import ucar.unidata.geoloc.projection.*;
import junit.framework.*;

/**
 *
 * @author John Caron
 * @version $Id: TestLatLonProjection.java,v 1.2 2005/02/19 22:17:54 caron Exp $
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

  void runCenter(double center) {
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

  public void testLatLonToProjBB() {
    runCenter( 110.45454545454547);
  }

}