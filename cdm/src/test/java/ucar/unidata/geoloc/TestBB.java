/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.geoloc;

import junit.framework.TestCase;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.nc2.util.Misc;

/**
 * Test Bounding Box utilities
 *
 * @author caron
 */
public class TestBB extends TestCase {

  void doTest(ProjectionImpl p, LatLonRect rect) {
    System.out.println("\n--Projection= "+p+" "+p.paramsToString());
    System.out.println("  llbb= "+rect.toString2());
    ProjectionRect prect = p.latLonToProjBB( rect);
    ProjectionRect prect2 = p.latLonToProjBB2( rect);
    System.out.println("  latLonToProjBB= "+prect);
    System.out.println("  latLonToProjBB2= "+prect2);
    assert equals( prect, prect2);
  }

  void doTests(ProjectionImpl p) {
    LatLonPointImpl ptL = new LatLonPointImpl(-10, 0.0);
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      ptL.setLongitude(lon);
      LatLonRect llbb = new LatLonRect(ptL, yinc, xinc);
      doTest(p, llbb);
    }
  }

  public void testLC() {
    doTests(new LambertConformal(40.0, 0, 20.0, 60.0));
  }

  boolean equals(ProjectionRect prect1, ProjectionRect prect2) {
    boolean b1 = equals( prect1.getLowerLeftPoint(), prect2.getLowerLeftPoint());
    boolean b2 = equals( prect1.getUpperRightPoint(), prect2.getUpperRightPoint());
    return b1 && b2;
  }

  boolean equals(ProjectionPoint pt1, ProjectionPoint pt2) {
    return Misc.closeEnough(pt1.getX(), pt2.getX()) &&  Misc.closeEnough(pt1.getY(), pt2.getY());
  }


}
