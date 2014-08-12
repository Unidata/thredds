/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

package ucar.unidata.geoloc.projection;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.sat.BoundingBoxHelper;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static ucar.unidata.geoloc.LatLonPointImmutable.INVALID;

/**
 * Sinusoidal projection, spherical earth.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 243
 *
 * @author John Caron
 * @since Feb 24, 2013
 */

public class Sinusoidal extends ProjectionImpl {
    private final double earthRadius, maxR, maxR2;
    private double centMeridian; // central Meridian in degrees
    private double falseEasting, falseNorthing;

    @Override
    public ProjectionImpl constructCopy() {
        ProjectionImpl result = new Sinusoidal(getCentMeridian(), getFalseEasting(), getFalseNorthing(),
                getEarthRadius());
        result.setDefaultMapArea(defaultMapArea);
        result.setName(name);
        return result;
    }

    /**
     * Constructor with default parameters
     */
    public Sinusoidal() {
        this(0.0, 0.0, 0.0, EARTH_RADIUS);
    }

    /**
     * Construct a Sinusoidal Projection.
     *
     * @param centMeridian   central Meridian (degrees)
     * @param false_easting  false_easting in km
     * @param false_northing false_northing in km
     * @param radius         earth radius in km
     */
    public Sinusoidal(double centMeridian, double false_easting, double false_northing, double radius) {
        super(CF.SINUSOIDAL, false);

        this.centMeridian = centMeridian;
        this.falseEasting = false_easting;
        this.falseNorthing = false_northing;
        this.earthRadius = radius;
        this.maxR2 = 2 * earthRadius * earthRadius;  // LOOK ??
        this.maxR = Math.sqrt(maxR2);

        addParameter(CF.GRID_MAPPING_NAME, CF.SINUSOIDAL);
        addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, centMeridian);
        addParameter(CF.EARTH_RADIUS, earthRadius * 1000);

        if ((false_easting != 0.0) || (false_northing != 0.0)) {
            addParameter(CF.FALSE_EASTING, false_easting);
            addParameter(CF.FALSE_NORTHING, false_northing);
            addParameter(CDM.UNITS, "km");
        }

    }

    /**
     * Get the central Meridian in degrees
     *
     * @return the central Meridian
     */
    public double getCentMeridian() {
        return centMeridian;
    }

    /**
     * Get the false easting, in km.
     *
     * @return the false easting.
     */
    public double getFalseEasting() {
        return falseEasting;
    }

    /**
     * Get the false northing, in km.
     *
     * @return the false northing.
     */
    public double getFalseNorthing() {
        return falseNorthing;
    }

    public double getEarthRadius() {
        return earthRadius;
    }

    //////////////////////////////////////////////
    // setters for IDV serialization - do not use except for object creating

    /**
     * Set the central Meridian
     *
     * @param centMeridian central Meridian in degrees
     */
    public void setCentMeridian(double centMeridian) {
        this.centMeridian = centMeridian;
    }

    /**
     * Set the false_easting, in km.
     * natural_x_coordinate + false_easting = x coordinate
     *
     * @param falseEasting x offset
     */
    public void setFalseEasting(double falseEasting) {
        this.falseEasting = falseEasting;
    }

    /**
     * Set the false northing, in km.
     * natural_y_coordinate + false_northing = y coordinate
     *
     * @param falseNorthing y offset
     */
    public void setFalseNorthing(double falseNorthing) {
        this.falseNorthing = falseNorthing;
    }

    /////////////////////////////////////////////////////


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Sinusoidal that = (Sinusoidal) o;

        if (Double.compare(that.centMeridian, centMeridian) != 0) {
            return false;
        }
        if (Double.compare(that.earthRadius, earthRadius) != 0) {
            return false;
        }
        if (Double.compare(that.falseEasting, falseEasting) != 0) {
            return false;
        }
        if (Double.compare(that.falseNorthing, falseNorthing) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = centMeridian != +0.0d ? Double.doubleToLongBits(centMeridian) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sinusoidal");
        sb.append("{earthRadius=").append(earthRadius);
        sb.append(", centMeridian=").append(centMeridian);
        sb.append(", falseEasting=").append(falseEasting);
        sb.append(", falseNorthing=").append(falseNorthing);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Get the parameters as a String
     *
     * @return the parameters as a String
     */
    public String paramsToString() {
        return toString();
    }

    /**
     * Does the line between these two points cross the projection "seam".
     *
     * @param pt1 the line goes between these two points
     * @param pt2 the line goes between these two points
     * @return false if there is no seam
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
        // either point is infinite
        if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2)) {
            return true;
        }

        // opposite signed long lines
        double x1 = pt1.getX() - falseEasting;
        double x2 = pt2.getX() - falseEasting;
        return (x1 * x2 < 0) && (Math.abs(x1 - x2) > earthRadius);
    }

    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
        double deltaLon_d = LatLonPointImpl.range180(latLon.getLongitude() - centMeridian);
        double fromLat_r = Math.toRadians(latLon.getLatitude());

        double toX = earthRadius * Math.toRadians(deltaLon_d) * Math.cos(fromLat_r);
        double toY = earthRadius * fromLat_r; // p 247 Snyder

        result.setLocation(toX + falseEasting, toY + falseNorthing);
        return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *
     * @param world  convert from these projection coordinates
     * @param result the object to write to
     * @return LatLonPoint the lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
        double fromX = world.getX() - falseEasting;
        double fromY = world.getY() - falseNorthing;

        double toLat_r = fromY / earthRadius;
        double toLon_r = Math.toRadians(centMeridian);
        if (!Misc.closeEnough(toLat_r, Math.PI / 2, 1e-10)) {
            toLon_r += fromX / (earthRadius * Math.cos(toLat_r));
        }

        if (Misc.closeEnough(toLon_r, Math.PI, 1e-10)) {
            toLon_r = Math.PI;
        }

        // Longitudes less than -pi or greater than +pi are off the earth.
        if (Math.abs(toLon_r) > Math.PI) {
            return INVALID;
        }

        result.setLatitude(Math.toDegrees(toLat_r));
        result.setLongitude(Math.toDegrees(toLon_r));
        return result;
    }

    /**
     * Create a ProjectionRect from the given LatLonRect.
     * Handles lat/lon points that do not intersect the projection panel.
     *
     * @param rect the LatLonRect
     * @return ProjectionRect, or null if no part of the LatLonRect intersects the projection plane
     */
    @Override
    public ProjectionRect latLonToProjBB(LatLonRect rect) {
        BoundingBoxHelper bbhelper = new BoundingBoxHelper(this, maxR);
        return bbhelper.latLonToProjBB(rect);
    }

    @Override
    public LatLonRect projToLatLonBB(ProjectionRect boundingBoxProj) {
//        ProjectionPoint lowerLeftProj  = boundingBoxProj.getLowerLeftPoint();
//        ProjectionPoint lowerRightProj = boundingBoxProj.getLowerRightPoint();
//        ProjectionPoint upperLeftProj  = boundingBoxProj.getUpperLeftPoint();
//        ProjectionPoint upperRightProj = boundingBoxProj.getUpperRightPoint();
//
//        // We need to find which projection points can be converted into valid lat/lon points.
//        List<ProjectionPoint> goodPoints = new LinkedList<>();
//
//        LatLonPoint lowerLeftLatLon  = projToLatLon(lowerLeftProj);
//        if (lowerLeftLatLon != INVALID) {
//            goodPoints.add(lowerLeftProj);
//        }
//
//        LatLonPoint lowerRightLatLon = projToLatLon(lowerRightProj);
//        if (lowerRightLatLon != INVALID) {
//            goodPoints.add(lowerRightProj);
//        }
//
//        LatLonPoint upperLeftLatLon  = projToLatLon(upperLeftProj);
//        if (upperLeftLatLon != INVALID) {
//            goodPoints.add(upperLeftProj);
//        }
//
//        LatLonPoint upperRightLatLon = projToLatLon(upperRightProj);
//        if (upperRightLatLon != INVALID) {
//            goodPoints.add(upperRightProj);
//        }
//
//
//        if (goodPoints.size() >= 3) {
//            // Do nothing; we have enough good points to make a bounding box.
//        } else if (goodPoints.size() == 2) {
//            if (lowerLeftLatLon != INVALID && lowerRightLatLon != INVALID) {
//
//            } else if (upperLeftLatLon != INVALID && upperRightLatLon != INVALID) {
//
//            } else if (lowerLeftLatLon != INVALID && upperLeftLatLon != INVALID) {
//
//            } else if (lowerRightLatLon != INVALID && upperRightLatLon != INVALID) {
//
//            }
//        } else if (goodPoints.size() == 1) {
//
//        } else {
//            // We cannot create a lat/lon bounding box from the supplied proj coords.
//            // LOOK: Is it a good idea to return null here? Maybe we need the concept of an "invalid bounding box"?
//            return null;
//        }



        // Convert the 4 corners of bounding box to lat/lon. These are in degrees, as is centMeridian.
        LatLonPoint lowerLeftLatLon  = projToLatLon(boundingBoxProj.getLowerLeftPoint());
        LatLonPoint lowerRightLatLon = projToLatLon(boundingBoxProj.getLowerRightPoint());
        LatLonPoint upperLeftLatLon  = projToLatLon(boundingBoxProj.getUpperLeftPoint());
        LatLonPoint upperRightLatLon = projToLatLon(boundingBoxProj.getUpperRightPoint());

        List<LatLonPoint> goodPoints = new LinkedList<>();
        for (LatLonPoint point : Arrays.asList(lowerLeftLatLon, lowerRightLatLon, upperLeftLatLon, upperRightLatLon)) {
            if (point != INVALID) {
                goodPoints.add(point);
            }
        }

        if (goodPoints.size() >= 3) {
            // Do nothing; we have enough good points to make a bounding box.
        } else if (goodPoints.size() == 2) {
            if (lowerLeftLatLon != INVALID && lowerRightLatLon != INVALID) {
                if (lowerLeftLatLon.getLongitude() <= centMeridian && lowerRightLatLon.getLongitude() >= centMeridian) {
                    // The lower corners straddle the central meridian, so we know that the maximum latitude of the
                    // bounding box will be 90 degrees.
                    goodPoints.add(new LatLonPointImpl(90, 0));  // The longitude doesn't matter.
                } else {
                    // We have the 2 lower corners of the bounding box.
                    // We need to extend it to the edge of the earth (lon == pi).
                    double lon_rad = Math.PI;  // Both upperLeftLatLon and upperRightLatLon will have this lon.

                    double lowerLeftProjX = boundingBoxProj.getLowerLeftPoint().getX();
                    double upperLeftLat_rad = calcLat_rad(lon_rad, lowerLeftProjX);  // In radians
                    upperLeftLatLon = new LatLonPointImpl(Math.toDegrees(upperLeftLat_rad), Math.toDegrees(lon_rad));
                    goodPoints.add(upperLeftLatLon);

                    double lowerRightProjX = boundingBoxProj.getLowerRightPoint().getX();
                    double upperRightLat_rad = calcLat_rad(lon_rad, lowerRightProjX);
                    upperRightLatLon = new LatLonPointImpl(Math.toDegrees(upperRightLat_rad), Math.toDegrees(lon_rad));
                    goodPoints.add(upperRightLatLon);
                }
            } else if (upperLeftLatLon != INVALID && upperRightLatLon != INVALID) {
                if (upperLeftLatLon.getLongitude() <= centMeridian && upperRightLatLon.getLongitude() >= centMeridian) {
                    // The upper corners straddle the central meridian, so we know that the minimum latitude of the
                    // bounding box will be -90 degrees.
                    goodPoints.add(new LatLonPointImpl(-90, 0));  // The longitude doesn't matter.
                } else {
                    // We have the 2 upper corners of the bounding box.
                    // We need to extend it to the edge of the earth (lon == -pi).
                    double lon = Math.PI;  // Both lowerLeftLatLon and lowerRightLatLon will have this lon.
                }
            }
        } else if (goodPoints.size() == 1) {

        } else {
            // We cannot create a lat/lon bounding box from the supplied proj coords.
            // LOOK: Is it a good idea to return null here? Maybe we need the concept of an "invalid bounding box"?
            return null;
        }

        return null;
    }

    private double calcLat_rad(double lon_rad, double x) {
        // This formula comes from solving formula 30-1 for phi. See Snyder p 247.
        double deltaLon_r = lon_rad - Math.toRadians(centMeridian);
        return Math.acos(x / (earthRadius * deltaLon_r));
    }


    /**
     * Calculates the minimum and maximum y-coords along the line {@code x = x0} that are still "on the earth".
     *
     * @param x0  defines a line that intersects the earth, in the projection coordinate system.
     * @return the minimum and maximum y-coords along the line {@code x = x0} that are still "on the earth".
     * @throws java.lang.IllegalArgumentException  if the line {@code x = x0} does not intersect the earth.
     */
    public double[] calcMinAndMaxYsAt(double x0) {
        LatLonPoint middleLatLon_d = new LatLonPointImpl(0, 0);
        ProjectionPoint middleProj = latLonToProj(middleLatLon_d);

        if (projToLatLon(x0, middleProj.getY()).equals(INVALID)) {
            throw new IllegalArgumentException("The line x = x0 does not intersect the earth. x0 = " + x0);
        }

        double limitLon_r = (x0 < middleProj.getX()) ? -Math.PI : Math.PI;
        double deltaLon_r = limitLon_r - Math.toRadians(centMeridian);

        // This formula comes from solving 30-1 for phi, and then plugging it into 30-2. See Snyder, p 247.
        double minY = -earthRadius * Math.acos(x0 / (earthRadius * deltaLon_r));
        double maxY = +earthRadius * Math.acos(x0 / (earthRadius * deltaLon_r));
        return new double[] { minY, maxY };
    }

    /**
     * Calculates the minimum and maximum x-coords along the line {@code y = y0} that are still "on the earth".
     *
     * @param y0 defines a line that intersects the earth, in the projection coordinate system.
     * @return the minimum and maximum x-coords along the line {@code y = y0} that are still "on the earth".
     * @throws java.lang.IllegalArgumentException if the line {@code y = y0} does not intersect the earth.
     */
    public double[] calcMinAndMaxXsAt(double y0) {
        LatLonPoint middleLatLon_d = new LatLonPointImpl(0, 0);
        ProjectionPoint middleProj = latLonToProj(middleLatLon_d);

        if (projToLatLon(middleProj.getX(), y0).equals(INVALID)) {
            throw new IllegalArgumentException("The line y = y0 does not intersect the earth. y0 = " + y0);
        }

        double minX = calcXat(y0, -Math.PI);
        double maxX = calcXat(y0, +Math.PI);
        return new double[] { minX, maxX };
    }

    private double calcXat(double y0, double lon_r) {
        // This formula comes from plugging 30-6 into 30-1. See Snyder, p 247-248.
        double deltaLon_r = lon_r - Math.toRadians(centMeridian);
        return earthRadius * deltaLon_r * Math.cos(y0 / earthRadius);
    }
}
