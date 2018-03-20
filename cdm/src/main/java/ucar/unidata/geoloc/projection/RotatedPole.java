/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
package ucar.unidata.geoloc.projection;

import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Rotated-pole longitude-latitude grid.
 *
 * This is probably the same as rotated lat lon, using matrix to do rotation.
 * Follows CF convention with "north_pole lat/lon", whereas RotatedLatLon uses south pole.
 *
 * @author Robert Schmunk
 * @author jcaron
 */
public class RotatedPole extends ProjectionImpl {
  private static final double RAD_PER_DEG = Math.PI / 180.;
  private static final double DEG_PER_RAD = 1. / RAD_PER_DEG;
  private static boolean show = false;

  /*	Coordinates of north pole for rotated pole. */
  private final ProjectionPointImpl northPole;

  /*	Y-axis rotation matrix. */
  private final double[][] rotY = new double[3][3];

  /*	Z-axis rotation matrix. */
  private final double[][] rotZ = new double[3][3];

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
    super("RotatedPole", false);

    northPole = new ProjectionPointImpl(northPoleLon, northPoleLat);
    buildRotationMatrices();

    addParameter(CF.GRID_MAPPING_NAME, CF.ROTATED_LATITUDE_LONGITUDE);
    addParameter(CF.GRID_NORTH_POLE_LATITUDE, northPoleLat);
    addParameter(CF.GRID_NORTH_POLE_LONGITUDE, northPoleLon);
  }

  private void buildRotationMatrices() {

    double betaRad = 0.;
    double gammaRad = 0.;

    if (northPole.getY() == 90.)
    {
        betaRad  = 0.;
        gammaRad = northPole.getX() * RAD_PER_DEG;
    }
    else
    {
        betaRad  = -(90. - northPole.getY()) * RAD_PER_DEG;
        gammaRad = (northPole.getX() + 180.) * RAD_PER_DEG;
    }

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

  public ProjectionPointImpl getNorthPole() {
    return new ProjectionPointImpl(northPole);
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new RotatedPole(northPole.getY(), northPole.getX());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
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
     return Math.abs(pt1.getX() - pt2.getX()) > 270.0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RotatedPole that = (RotatedPole) o;
    return northPole.equals(that.northPole);
  }

  @Override
  public int hashCode() {
    return northPole.hashCode();
  }

}
