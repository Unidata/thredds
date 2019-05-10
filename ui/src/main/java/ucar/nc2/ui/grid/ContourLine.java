/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grid;

import ucar.nc2.ui.gis.GisPart;

import java.awt.geom.*;  // for Point2D.Double
import java.util.*;      // for Iterator and ArrayList

/* ContourLine
 * Encapsulates a contour line, with a GisPart,a contour value,
 * and a boolean indicator if this line was edited or is a new line.
 *
 * This holds one single connected contour line. It does not hold
 * several unconnected contour lines for the same grid even if there
 * are several such line of the same contour value. Each one has one ContourLine
 * instance.
 *
 * Does not know anything about any display method, particular coordinate system,
 * or line labels.
  *
 * @author wier
 */

public class ContourLine implements GisPart {
  private double contourLevel; // the contour's level or value
  private boolean newLineFlag = false; // true if this line is new or edited
  int npts;
  double [] wx;  // contour line positions found in ContourGrid class
  double [] wy;

  /*
  * Constructors for the ContourLine object for this input data.
  * The object contains a polyline of straight line segments,
  * the contour's level or value, and a "moddifed" indicator
  * used for programs which can edit contour lines and
  * and work with contour lines that have been editied.
  * "ifNew" input argument is optional.
  * points is an ArrayList of Point2D.Double
  */
  public ContourLine(List<Point2D.Double> points, double level) {
    this.npts = points.size();
    // define coordinate array size for this line
    wx = new double[npts];
    wy = new double[npts];

    // get each coord pair and push into the local x y arrays
    for (int i = 0; i < points.size(); i++) {
      Point2D.Double onexypair = (points.get(i));
      wx[i] = onexypair.getX();
      wy[i] = onexypair.getY();
    }
    contourLevel = level;
  }

  // constructor with indicator if contour Line is new or modified;
  // used for systems with editable contour lines.
  public ContourLine(ArrayList points, double level, boolean ifNew) {
    this(points, level);
    newLineFlag = ifNew;
  }

  // implement GisPart methods
  public int getNumPoints() {
    return npts;
  }

  public double[] getX() {
    return wx;
  }

  public double[] getY() {
    return wy;
  }

  /*
  * contourLevel is returned, the contour's value
  */
  public double getContourLevel() {
    return contourLevel;
  }

  /*
  * "newLineFlag" is returned, indicator if this contour is new or edited
  */
  public boolean isNewLine() {
    return newLineFlag;
  }

  /*
  * set the "newLineFlag" flag,
  * the indicator if this contour is new or just edited
  */
  public void setNewLineFlag(boolean newflag) {
    newLineFlag = newflag;
  }

}  // end ContourLine class

