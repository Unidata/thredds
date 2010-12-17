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
/*
 *	This software is provided by the National Aeronatics and Space
 *	Administration Goddard Institute for Space Studies (NASA GISS) for full,
 *	free and open release. It is understood by the recipient/user that NASA
 *	assumes no liability for any errors contained in the code.  Although this
 *	software is released without conditions or restrictions in its use, it is
 *	expected that appropriate credit be given to its author and to NASA GISS
 *	should the software be included by the recipient in acknowledgments or
 *	credits in other product development.
 */

/*
 *
 * Copyright 2007 Unidata Program Center/University Corporation for
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
package ucar.unidata.geoloc.projection;

import java.awt.geom.Point2D;
import ucar.unidata.geoloc.*;

/**
 * Rotated-pole longitude-latitude grid.
 *
 * @author Robert Schmunk
 * @author jcaron
 */
public class RotatedPole extends ProjectionImpl {
  private static final double RAD_PER_DEG = Math.PI / 180.;
  private static final double DEG_PER_RAD = 1. / RAD_PER_DEG;
  private static boolean show = false;

  /*	Coordinates of north pole for rotated pole. */
  private ProjectionPointImpl northPole;

  /*	Y-axis rotation matrix. */
  private double[][] rotY = new double[3][3];

  /*	Z-axis rotation matrix. */
  private double[][] rotZ = new double[3][3];

  /**
   * Default Constructor, needed for beans.
   */
  public RotatedPole() {
    this(0.0, 0.0);
  }

  /**
   * Constructor.
   * @param northPoleLat
   * @param northPoleLon
   */
  public RotatedPole(double northPoleLat, double northPoleLon) {
    northPole = new ProjectionPointImpl(northPoleLon, northPoleLat);
    buildRotationMatrices();

    addParameter(ATTR_NAME, "rotated_latitude_longitude");
    addParameter("grid_north_pole_latitude", northPoleLat);
    addParameter("grid_north_pole_longitude", northPoleLon);
  }

  private void buildRotationMatrices() {
    //	Rotation angles and matrices.
    double betaRad = -(90. - northPole.y) * RAD_PER_DEG;
    double gammaRad = (northPole.x + 180.) * RAD_PER_DEG;

    double cosBeta = Math.cos(betaRad);
    double sinBeta = Math.sin(betaRad);

    double cosGamma = Math.cos(gammaRad);
    double sinGamma = Math.sin(gammaRad);

    rotY[0][0] = cosBeta;
    rotY[0][1] = 0.;
    rotY[0][2] = -sinBeta;
    rotY[1][0] = 0.;
    rotY[1][1] = 1.;
    rotY[1][2] = 0.;
    rotY[2][0] = sinBeta;
    rotY[2][1] = 0.;
    rotY[2][2] = cosBeta;

    rotZ[0][0] = cosGamma;
    rotZ[0][1] = sinGamma;
    rotZ[0][2] = 0.;
    rotZ[1][0] = -sinGamma;
    rotZ[1][1] = cosGamma;
    rotZ[1][2] = 0.;
    rotZ[2][0] = 0.;
    rotZ[2][1] = 0.;
    rotZ[2][2] = 1.;
  }

  public Point2D.Double getNorthPole() {
    return (Point2D.Double) northPole.clone();
  }

  /**
   * copy constructor - avoid clone !!
   */
  public ProjectionImpl constructCopy() {
    return new RotatedPole(northPole.getY(), northPole.getX());
  }

  public String paramsToString() {
    return " northPole= " + northPole;
  }

  /**
   * Transform a "real" longitude and latitude into the rotated longitude (X) and
   * rotated latitude (Y).
   */
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint) {
    double lat = latlon.getLatitude();
    double lon = latlon.getLongitude();

    //	Lon-lat pair to xyz coordinates on sphere with radius 1
    double[] p0 = new double[]
            {Math.cos(lat * RAD_PER_DEG) * Math.cos(lon * RAD_PER_DEG),
                    Math.cos(lat * RAD_PER_DEG) * Math.sin(lon * RAD_PER_DEG),
                    Math.sin(lat * RAD_PER_DEG)};

    //	Rotate around Z-axis
    double[] p1 = new double[]
            {rotZ[0][0] * p0[0] + rotZ[0][1] * p0[1] + rotZ[0][2] * p0[2],
                    rotZ[1][0] * p0[0] + rotZ[1][1] * p0[1] + rotZ[1][2] * p0[2],
                    rotZ[2][0] * p0[0] + rotZ[2][1] * p0[1] + rotZ[2][2] * p0[2]};

    //	Rotate around Y-axis
    double[] p2 = new double[]
            {rotY[0][0] * p1[0] + rotY[0][1] * p1[1] + rotY[0][2] * p1[2],
                    rotY[1][0] * p1[0] + rotY[1][1] * p1[1] + rotY[1][2] * p1[2],
                    rotY[2][0] * p1[0] + rotY[2][1] * p1[1] + rotY[2][2] * p1[2]};

    final double lonR = LatLonPointImpl.range180( Math.atan2(p2[1], p2[0]) * DEG_PER_RAD);
    //final double lonR = Math.atan2(p2[1], p2[0]) * DEG_PER_RAD;
    final double latR = Math.asin(p2[2]) * DEG_PER_RAD;

    if (destPoint == null)
      destPoint =  new ProjectionPointImpl(lonR, latR);
    else
      destPoint.setLocation(lonR, latR);

     if (show)
        System.out.println("LatLon= " + latlon+" proj= " + destPoint);

    return destPoint;
  }

  /**
   * Transform a rotated longitude (X) and rotated latitude (Y) into a "real"
   * longitude-latitude pair.
   */
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    //	"x" and "y" input for rotated pole coords are actually a lon-lat pair
    final double lonR = LatLonPointImpl.range180( ppt.getX()); // LOOK guessing
    final double latR = ppt.getY();

    //	Lon-lat pair to xyz coordinates on sphere with radius 1
    double[] p0 = new double[]
            {Math.cos(latR * RAD_PER_DEG) * Math.cos(lonR * RAD_PER_DEG),
                    Math.cos(latR * RAD_PER_DEG) * Math.sin(lonR * RAD_PER_DEG),
                    Math.sin(latR * RAD_PER_DEG)};

    //	Inverse rotate around Y-axis (using transpose of Y matrix)
    double[] p1 = new double[]
            {rotY[0][0] * p0[0] + rotY[1][0] * p0[1] + rotY[2][0] * p0[2],
                    rotY[0][1] * p0[0] + rotY[1][1] * p0[1] + rotY[2][1] * p0[2],
                    rotY[0][2] * p0[0] + rotY[1][2] * p0[1] + rotY[2][2] * p0[2]};

    //	Inverse rotate around Z-axis (using transpose of Z matrix)
    double[] p2 = new double[]
            {rotZ[0][0] * p1[0] + rotZ[1][0] * p1[1] + rotZ[2][0] * p1[2],
                    rotZ[0][1] * p1[0] + rotZ[1][1] * p1[1] + rotZ[2][1] * p1[2],
                    rotZ[0][2] * p1[0] + rotZ[1][2] * p1[1] + rotZ[2][2] * p1[2]};

    final double lon = Math.atan2(p2[1], p2[0]) * DEG_PER_RAD;
    final double lat = Math.asin(p2[2]) * DEG_PER_RAD;

    if (destPoint == null)
      destPoint = new LatLonPointImpl(lat, lon);
    else
      destPoint.set(lat, lon);

    if (show)
       System.out.println("Proj= " + ppt+" latlon= " + destPoint);

    return destPoint;
  }

  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
     return Math.abs(pt1.getX() - pt2.getX()) > 270.0;  // ?? LOOK: do I believe this ??
  }

  public boolean equals(Object proj) {
    if (!(proj instanceof RotatedPole)) {
      return false;
    }

    RotatedPole oo = (RotatedPole) proj;
    return this.getNorthPole().equals(oo.getNorthPole());
  }

}
