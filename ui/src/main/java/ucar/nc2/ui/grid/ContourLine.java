// $Id: ContourLine.java 50 2006-07-12 16:30:06Z caron $
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
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
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
  public ContourLine(ArrayList points, double level) {
    this.npts = points.size();
    // define coordinate array size for this line
    wx = new double[npts];
    wy = new double[npts];

    // get each coord pair and push into the local x y arrays
    Point2D.Double onexypair = new Point2D.Double();
    for (int i = 0; i < points.size(); i++) {
      onexypair = (Point2D.Double) (points.get(i));
      wx[i] = onexypair.getX();
      wy[i] = onexypair.getY();
      //System.out.println("  i="+i+"  ("
      //		   +(((Point2D.Double)(points.get(i))).getX())
      //		   +","+wy[i]+")");
    }
    //System.out.println("  end of line");
    contourLevel = level;
  }

  // constructor with indicator if contour Line is new or modified;
  // used for systems with editable contour lines.
  public ContourLine(ArrayList points, double level, boolean ifNew) {
    this.npts = points.size();
    // define coordinate array size for this line
    wx = new double[npts];
    wy = new double[npts];

    // get one coord pait and push into the local x y arrays
    Point2D.Double onexypair = new Point2D.Double();
    for (int i = 0; i < points.size(); i++) {
      onexypair = (Point2D.Double) (points.get(i));
      wx[i] = onexypair.getX();
      wy[i] = onexypair.getY();
    }

    contourLevel = level;
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

