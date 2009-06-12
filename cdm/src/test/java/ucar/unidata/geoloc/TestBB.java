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

import junit.framework.TestCase;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.nc2.util.Misc;

/**
 * compare ProjectionImpl.latLonToProjBB against latLonToProjBB2
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
