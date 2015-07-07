/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.unidata.geoloc.projection.sat;

import ucar.unidata.geoloc.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for finding the ProjectionRect from a LatLonRect
 *
 * @author caron
 * @since 7/29/2014
 */
public class BoundingBoxHelper {

  private double maxR, maxR2;
  private Projection proj;

  public BoundingBoxHelper( Projection proj, double maxR) {
    this.proj = proj;
    this.maxR = maxR;
    this.maxR2 = maxR * maxR;
  }

  public ProjectionRect latLonToProjBB(LatLonRect rect) {

    ProjectionPoint llpt = proj.latLonToProj(rect.getLowerLeftPoint(), new ProjectionPointImpl());
    ProjectionPoint urpt = proj.latLonToProj(rect.getUpperRightPoint(), new ProjectionPointImpl());
    ProjectionPoint lrpt = proj.latLonToProj(rect.getLowerRightPoint(), new ProjectionPointImpl());
    ProjectionPoint ulpt = proj.latLonToProj(rect.getUpperLeftPoint(), new ProjectionPointImpl());

    // how many are bad?
    List<ProjectionPoint> goodPts = new ArrayList<>(4);
    int countBad = 0;
    if (!addGoodPts(goodPts, llpt))
      countBad++;
    if (!addGoodPts(goodPts, urpt))
      countBad++;
    if (!addGoodPts(goodPts, lrpt))
      countBad++;
    if (!addGoodPts(goodPts, ulpt))
      countBad++;

    // case : 3 or 4 good points, just use those

    // case: only 2 good ones : extend to edge of the limit circle
    if (countBad == 2) {

      if (!ProjectionPointImpl.isInfinite(llpt) && !ProjectionPointImpl.isInfinite(lrpt)) {
        addGoodPts(goodPts, new ProjectionPointImpl(0, maxR));

      } else if (!ProjectionPointImpl.isInfinite(ulpt) && !ProjectionPointImpl.isInfinite(llpt)) {
        addGoodPts(goodPts, new ProjectionPointImpl(maxR, 0));

      } else if (!ProjectionPointImpl.isInfinite(ulpt) && !ProjectionPointImpl.isInfinite(urpt)) {
        addGoodPts(goodPts, new ProjectionPointImpl(0, -maxR));

      } else if (!ProjectionPointImpl.isInfinite(urpt) && !ProjectionPointImpl.isInfinite(lrpt)) {
        addGoodPts(goodPts, new ProjectionPointImpl(-maxR, 0));

      } else {
        throw new IllegalStateException();
      }

    } else if (countBad == 3) { // case: only 1 good one : extend to wedge of the limit circle

      if (!ProjectionPointImpl.isInfinite(llpt)) {
        double xcoord = llpt.getX();
        addGoodPts(goodPts, new ProjectionPointImpl(xcoord, getLimitCoord(xcoord)));

        double ycoord = llpt.getY();
        addGoodPts(goodPts, new ProjectionPointImpl(getLimitCoord(ycoord), ycoord));
      } else if (!ProjectionPointImpl.isInfinite(urpt)) {
        double xcoord = urpt.getX();
        addGoodPts(goodPts, new ProjectionPointImpl(xcoord, -getLimitCoord(xcoord)));

        double ycoord = urpt.getY();
        addGoodPts(goodPts, new ProjectionPointImpl(-getLimitCoord(ycoord), ycoord));
      } else if (!ProjectionPointImpl.isInfinite(ulpt)) {
        double xcoord = ulpt.getX();
        addGoodPts(goodPts, new ProjectionPointImpl(xcoord, -getLimitCoord(xcoord)));

        double ycoord = ulpt.getY();
        addGoodPts(goodPts, new ProjectionPointImpl(getLimitCoord(ycoord), ycoord));
      } else if (!ProjectionPointImpl.isInfinite(lrpt)) {
        double xcoord = lrpt.getX();
        addGoodPts(goodPts, new ProjectionPointImpl(xcoord, getLimitCoord(xcoord)));

        double ycoord = lrpt.getY();
        addGoodPts(goodPts, new ProjectionPointImpl(-getLimitCoord(ycoord), ycoord));

      } else {
        throw new IllegalStateException();
      }

    }

    return makeRect(goodPts);
  }

  private boolean addGoodPts(List<ProjectionPoint> goodPts, ProjectionPoint pt) {
    if (!ProjectionPointImpl.isInfinite(pt)) {
      goodPts.add(pt);
      //System.out.println("  good= "+pt);
      return true;
    } else return false;
  }

  // where does line x|y = coord intersect the map limit circle?
  // return the positive root.
  private double getLimitCoord(double coord) {
    return Math.sqrt(maxR2 - coord * coord);
  }

  private ProjectionRect makeRect(List<ProjectionPoint> goodPts) {
    double minx = Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxx = -Double.MAX_VALUE;
    double maxy = -Double.MAX_VALUE;
    for (ProjectionPoint pp : goodPts) {
      minx = Math.min(minx, pp.getX());
      maxx = Math.max(maxx, pp.getX());
      miny = Math.min(miny, pp.getY());
      maxy = Math.max(maxy, pp.getY());
    }
    return new ProjectionRect(minx, miny, maxx, maxy);
  }
}
