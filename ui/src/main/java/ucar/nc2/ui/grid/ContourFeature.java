/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grid;

import ucar.nc2.ui.gis.AbstractGisFeature;
import ucar.nc2.ui.gis.GisPart;

import java.util.*;      // for Iterator and ArrayList
import java.awt.geom.*;  // for Point2D.Double

/**
 * An AbstractGisFeature derived class for contour lines.
 * <p/>
 * Created:
 *
 * @author wier
 */

public class ContourFeature extends AbstractGisFeature {
  private int numparts, npts;
  private ArrayList<ContourLine> lines; // arrayList of ContourLines
  private double contourValue;

  /**
   * constructor
   */
  public ContourFeature(List<ContourLine> conLines) {

    // set how many GisParts there are
    numparts = conLines.size();

    // save the input ContourLines as member data
    lines = new ArrayList<>(conLines);

    // save the single contour value for all lines here as member data
    if (conLines.size() > 0)
      contourValue = lines.get(0).getContourLevel();
    else
      contourValue = 0.0;

    // using every line, add together how many points on all the lines
    int np, count = 0;
    for (int i = 0; i < numparts; i++) {
      GisPart cl = conLines.get(i);
      np = cl.getNumPoints();
      count += np;

      // error if contour values not all the same
      if (lines.get(i).getContourLevel() != contourValue)
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

  // implement GisFeature methods:

  public java.awt.geom.Rectangle2D getBounds2D() {
    // Get the bounding box for this feature. from java.awt.geom.Rectangle2D
    double x0 = lines.get(0).getX()[0];
    double y0 = lines.get(0).getY()[0];
    double xMaxInd = x0, xmin = x0, yMaxInd = y0, ymin = y0;

    for (Object line : lines) {
      GisPart cline = (ContourLine) line;
      double[] xpts = cline.getX();
      double[] ypts = cline.getY();
      for (int j = 0; j < cline.getNumPoints(); j++) {
        if (xpts[j] < xmin) {
          xmin = xpts[j];
        } else if (xpts[j] > xMaxInd) {
          xMaxInd = xpts[j];
        }
        if (ypts[j] < ymin) {
          ymin = ypts[j];
        } else if (ypts[j] > yMaxInd) {
          yMaxInd = ypts[j];
        }
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


