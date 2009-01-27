// $Id: ContourFeature.java 50 2006-07-12 16:30:06Z caron $
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
package ucar.nc2.ui.grid;

import thredds.datamodel.gis.*;

import java.util.*;      // for Iterator and ArrayList
import java.awt.geom.*;  // for Point2D.Double

/**
 * An AbstractGisFeature derived class for contour lines.
 * <p/>
 * Created:
 *
 * @author wier
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */

public class ContourFeature extends thredds.datamodel.gis.AbstractGisFeature {
  private int numparts, npts;
  private ArrayList lines; // arrayList of ContourLines
  private double contourValue;

  /**
   * constructor
   */
  public ContourFeature(ArrayList conLines) {

    // set how many GisParts there are
    numparts = conLines.size();

    // save the input ContourLines as member data
    lines = new ArrayList(conLines);

    // save the single contour value for all lines here as member data
    if (conLines.size() > 0)
      contourValue = ((ContourLine) (lines.get(0))).getContourLevel();
    else
      contourValue = 0.0;

    // using every line, add together how many points on all the lines
    int np, count = 0;
    for (int i = 0; i < numparts; i++) {
      GisPart cl = (GisPart) (conLines.get(i));
      np = cl.getNumPoints();
      count += np;

      // error if contour values not all the same
      if (((ContourLine) (lines.get(i))).getContourLevel() != contourValue)
        System.out.println("  Mismatch: all contour levels" +
                " in one ContourFeature should be the same.");
    }
    npts = count;
  }  // end cstr


  /**
   * Get the value of the contour level for all the contour lines
   * in this object.
   *
   * @return double value
   */
  public double getContourValue() {
    return contourValue;
  }

  ;

  // implement GisFeature methods:

  public java.awt.geom.Rectangle2D getBounds2D() {
    // Get the bounding box for this feature. from java.awt.geom.Rectangle2D
    double x0 = (((ContourLine) (lines.get(0))).getX())[0];
    double y0 = (((ContourLine) (lines.get(0))).getY())[0];
    double xMaxInd = x0, xmin = x0, yMaxInd = y0, ymin = y0;

    for (int i = 0; i < lines.size(); i++) {
      GisPart cline = (ContourLine) (lines.get(i));
      double[] xpts = cline.getX();
      double[] ypts = cline.getY();
      for (int j = 0; j < cline.getNumPoints(); j++) {
        if (xpts[j] < xmin)
          xmin = xpts[j];
        else if (xpts[j] > xMaxInd)
          xMaxInd = xpts[j];
        if (ypts[j] < ymin)
          ymin = ypts[j];
        else if (ypts[j] > yMaxInd)
          yMaxInd = ypts[j];
      }
    }

    // Rectangle2D.Double(double x, double y, double width, double height)
    Rectangle2D.Double rect =
            new Rectangle2D.Double(xmin, ymin, xMaxInd - xmin, yMaxInd - ymin);
    return rect;
  }

  public java.util.Iterator getGisParts() {
    return lines.iterator();
  }

  public int getNumParts() {
    return numparts;
  }

  public int getNumPoints() {
    return npts;
  }

} // ContourFeature


