/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.projection.LambertConformal;

import java.lang.invoke.MethodHandles;

/**
 * compare ProjectionImpl.latLonToProjBB against latLonToProjBB2
 * There are a lot of failures - not sure why, but probably latLonToProjBB2 is wrong.
 *
 * @author caron
 */
public class TestLatLonToProjBB extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  void doTest(ProjectionImpl p, LatLonRect rect) {
    ProjectionRect prect = p.latLonToProjBB( rect);
    ProjectionRect prect2 = p.latLonToProjBB2( rect);
    if (!prect.nearlyEquals(prect2)) {
      System.out.println("\nFAIL Projection= " + p);
      System.out.println("  llbb= " + rect.toString2());
      System.out.println("  latLonToProjBB= " + prect);
      System.out.println("  latLonToProjBB2= " + prect2);
      assert false;
    }
  }

  void doTests(ProjectionImpl p, double mid) {
    LatLonPointImpl ptL = new LatLonPointImpl(-10, 0.0);
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = mid - 90; lon < mid + 90; lon += xinc) {
      ptL.setLongitude(lon);
      LatLonRect llbb = new LatLonRect(ptL, yinc, xinc);
      
      ProjectionPoint pt1 = p.latLonToProj(ptL);
      ProjectionPoint pt2 = p.latLonToProj(llbb.getLowerRightPoint());
      if (!p.crossSeam(pt1, pt2))
        doTest(p, llbb);
    }
  }

  public void testLC() {
    doTests(new LambertConformal(40.0, 0, 20.0, 60.0), 0);
  }

  public void utestProblem() {
    ProjectionImpl p = new LambertConformal(40.0, 0, 20.0, 60.0);
    LatLonPointImpl ptL = new LatLonPointImpl(-10, 135);
    LatLonPointImpl ptL2 = new LatLonPointImpl(10, 157.5);
    LatLonRect llbb = new LatLonRect(ptL, ptL2);

    ProjectionPoint pt1 = p.latLonToProj(ptL);
    ProjectionPoint pt2 = p.latLonToProj(ptL2);
    System.out.println("pt1 = "+pt1);
    System.out.println("pt2 = "+pt2);

    ProjectionPoint lr = p.latLonToProj(llbb.getLowerRightPoint());
    System.out.println("lr = "+lr);
    if (!p.crossSeam(pt1, pt2))
      doTest(p, llbb);

  } 
}
