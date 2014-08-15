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

import com.google.common.math.DoubleMath;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;

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
    private final double earthRadius;
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
        double fromX = world.getX() - falseEasting;
        double fromY = world.getY() - falseNorthing;

        double toLat_r = fromY / earthRadius;
        double toLon_r;

        if (Misc.closeEnough(Math.abs(toLat_r), PI_OVER_2, 1e-10)) {
            toLat_r = toLat_r < 0 ? -PI_OVER_2 : +PI_OVER_2;
            toLon_r = Math.toRadians(centMeridian);  // if lat == +- pi/2, set lon = centMeridian (Snyder 248)
        } else if (Math.abs(toLat_r) < PI_OVER_2) {
            toLon_r = Math.toRadians(centMeridian) + fromX / (earthRadius * Math.cos(toLat_r));
        } else {
            return INVALID;  // Projection point is off the earth.
        }

        if (Misc.closeEnough(Math.abs(toLon_r), PI, 1e-10)) {
            toLon_r = toLon_r < 0 ? -PI : +PI;
        } else if (Math.abs(toLon_r) > PI) {
            return INVALID;  // Projection point is off the earth.
        }

        result.setLatitude(Math.toDegrees(toLat_r));
        result.setLongitude(Math.toDegrees(toLon_r));
        return result;
    }

    @Override
    public LatLonRect projToLatLonBB(ProjectionRect projBB) {
        List<ProjectionPoint> goodPoints = new LinkedList<>();

        ProjectionPoint northPole = latLonToProj(new LatLonPointImpl(90, 0));
        if (projBB.contains(northPole)) {
            goodPoints.add(northPole);
        }

        ProjectionPoint southPole = latLonToProj(new LatLonPointImpl(-90, 0));
        if (projBB.contains(southPole)) {
            goodPoints.add(southPole);
        }

        if (goodPoints.size() == 2) {  // projBB contains both north and south poles, and thus, the entire earth.
            return new LatLonRect(new LatLonPointImpl(-90, -180), new LatLonPointImpl(90, 180));
        }


//        ProjectionPoint lowerLeftProj  = projBB.getLowerLeftPoint();
//        ProjectionPoint lowerRightProj = projBB.getLowerRightPoint();
//        ProjectionPoint upperLeftProj  = projBB.getUpperLeftPoint();
//        ProjectionPoint upperRightProj = projBB.getUpperRightPoint();
//
//        for (ProjectionPoint point : Arrays.asList(lowerLeftProj, lowerRightProj, upperLeftProj, upperRightProj)) {
//            if (projToLatLon(point) != INVALID) {
//                goodPoints.add(point);
//            }
//        }

        goodPoints.addAll(getMapEdgeIntercepts(projBB));

//        if (goodPoints.size() >= 3) {
//            // Do nothing; we have enough good points to make a bounding box.
//        } else if (goodPoints.size() == 2) {
//            if (projToLatLon(lowerLeftProj) != INVALID && projToLatLon(lowerRightProj) != INVALID) {
//                // Valid bottom: need upperLeftProj and upperRightProj.
//                goodPoints.add(new ProjectionPointImpl(lowerLeftProj.getX(), calcMaxYAt(lowerLeftProj.getX())));
//                goodPoints.add(new ProjectionPointImpl(lowerRightProj.getX(), calcMaxYAt(lowerRightProj.getX())));
//            } else if (projToLatLon(upperLeftProj) != INVALID && projToLatLon(upperRightProj) != INVALID) {
//                // Valid top: need lowerLeftProj and lowerRightProj.
//                goodPoints.add(new ProjectionPointImpl(upperLeftProj.getX(), calcMinYAt(upperLeftProj.getX())));
//                goodPoints.add(new ProjectionPointImpl(upperRightProj.getX(), calcMinYAt(upperRightProj.getX())));
//            } else if (projToLatLon(lowerLeftProj) != INVALID && projToLatLon(upperLeftProj) != INVALID) {
//                // Valid left: need lowerRightProj and upperRightProj.
//                goodPoints.add(new ProjectionPointImpl(calcMaxXAt(lowerLeftProj.getY()), lowerLeftProj.getY()));
//                goodPoints.add(new ProjectionPointImpl(calcMaxXAt(upperLeftProj.getY()), upperLeftProj.getY()));
//            } else if (projToLatLon(lowerRightProj) != INVALID && projToLatLon(upperRightProj) != INVALID) {
//                // Valid right: need lowerLeftProj and upperLeftProj.
//                goodPoints.add(new ProjectionPointImpl(calcMinXAt(lowerRightProj.getY()), lowerRightProj.getY()));
//                goodPoints.add(new ProjectionPointImpl(calcMinXAt(upperRightProj.getY()), upperRightProj.getY()));
//            } else {
//                throw new AssertionError("CAN'T HAPPEN: We checked all possible pairings of good points." +
//                        "Two good points on opposite corners is impossible.");
//            }
//        } else if (goodPoints.size() == 1) {
//            if (projToLatLon(lowerLeftProj) != INVALID) {
//                // Valid lower-left: need upperLeftProj and lowerRightProj.
//                goodPoints.add(new ProjectionPointImpl(lowerLeftProj.getX(), calcMaxYAt(lowerLeftProj.getX())));
//                goodPoints.add(new ProjectionPointImpl(calcMaxXAt(lowerLeftProj.getY()), lowerLeftProj.getY()));
//            } else if (projToLatLon(lowerRightProj) != INVALID) {
//                // Valid lower-right: need upperRightProj and lowerLeftProj.
//                goodPoints.add(new ProjectionPointImpl(lowerRightProj.getX(), calcMaxYAt(lowerRightProj.getX())));
//                goodPoints.add(new ProjectionPointImpl(calcMinXAt(lowerRightProj.getY()), lowerRightProj.getY()));
//            } else if (projToLatLon(upperLeftProj) != INVALID) {
//                // Valid upper-left: need lowerLeftProj and upperRightProj.
//                goodPoints.add(new ProjectionPointImpl(upperLeftProj.getX(), calcMinYAt(upperLeftProj.getX())));
//                goodPoints.add(new ProjectionPointImpl(calcMaxXAt(upperLeftProj.getY()), upperLeftProj.getY()));
//            } else if (projToLatLon(upperRightProj) != INVALID) {
//                // Valid upper-right: need lowerRightProj and upperleftProj.
//                goodPoints.add(new ProjectionPointImpl(upperRightProj.getX(), calcMinYAt(upperRightProj.getX())));
//                goodPoints.add(new ProjectionPointImpl(calcMinXAt(upperRightProj.getY()), upperRightProj.getY()));
//            } else {
//                throw new AssertionError("CAN'T HAPPEN: We checked all points. One of them should have been valid.");
//            }
//        } else {
//            throw new IllegalArgumentException("The bounding box doesn't intercept the earth: " + projBB);
//        }

        return makeLatLonRect(goodPoints);
    }

    public List<ProjectionPoint> getMapEdgeIntercepts(ProjectionRect projBB) {
        List<ProjectionPoint> intercepts = new LinkedList<>();

        for (ProjectionPoint topIntercept : getMapEdgeInterceptsAtY(projBB.getUpperRightPoint().getY())) {
            if (pointIsBetween(topIntercept, projBB.getUpperLeftPoint(), projBB.getUpperRightPoint())) {
                intercepts.add(topIntercept);
            }
        }

        for (ProjectionPoint rightIntercept : getMapEdgeInterceptsAtX(projBB.getUpperRightPoint().getX())) {
            if (pointIsBetween(rightIntercept, projBB.getUpperRightPoint(), projBB.getLowerRightPoint())) {
                intercepts.add(rightIntercept);
            }
        }

        for (ProjectionPoint bottomIntercept : getMapEdgeInterceptsAtY(projBB.getLowerLeftPoint().getY())) {
            if (pointIsBetween(bottomIntercept, projBB.getLowerLeftPoint(), projBB.getLowerRightPoint())) {
                intercepts.add(bottomIntercept);
            }
        }

        for (ProjectionPoint leftIntercept : getMapEdgeInterceptsAtX(projBB.getLowerLeftPoint().getX())) {
            if (pointIsBetween(leftIntercept, projBB.getLowerLeftPoint(), projBB.getUpperLeftPoint())) {
                intercepts.add(leftIntercept);
            }
        }

        return intercepts;
    }

    private boolean pointIsBetween(ProjectionPoint point, ProjectionPoint linePoint1, ProjectionPoint linePoint2) {
        if (linePoint1.getX() == linePoint2.getX()) {  // No fuzzy comparison necessary.
            assert point.getX() == linePoint1.getX() : "point should have the same X as the line.";

            double minY = Math.min(linePoint1.getY(), linePoint2.getY());
            double maxY = Math.min(linePoint1.getY(), linePoint2.getY());

            // Returns true if point.getY() is in the range [minY, maxY], with fuzzy math.
            return DoubleMath.fuzzyCompare(minY, point.getY(), TOLERANCE) <= 0 &&
                   DoubleMath.fuzzyCompare(point.getY(), maxY, TOLERANCE) <= 0;
        } else if (linePoint1.getY() == linePoint2.getY()) {  // No fuzzy comparison necessary.
            assert point.getY() == linePoint1.getY() : "point should have the same Y as the line.";

            double minX = Math.min(linePoint1.getX(), linePoint2.getX());
            double maxX = Math.min(linePoint1.getX(), linePoint2.getX());

            // Returns true if point.getX() is in the range [minX, maxX], with fuzzy math.
            return DoubleMath.fuzzyCompare(minX, point.getX(), TOLERANCE) <= 0 &&
                   DoubleMath.fuzzyCompare(point.getX(), maxX, TOLERANCE) <= 0;
        } else {
            throw new AssertionError("CAN'T HAPPEN: linePoint1 and linePoint2 are corners on the same side of a " +
                    "bounding box; they must have *identical* x or y values.");
        }
    }

    /**
     * Calculates the minimum y-coord along the line {@code x = x0} that is still "on the earth".
     *
     * @param x0 defines a line that intersects the earth, in the projection coordinate system.
     * @return the minimum y-coord along the line {@code x = x0} that is still "on the earth".
     * @throws IllegalArgumentException if the line {@code x = x0} does not intersect the earth.
     */
    public double calcMinYAt(double x0) throws IllegalArgumentException {
        return getLimitYsAt(x0)[0];
    }

    /**
     * Calculates the maximum y-coord along the line {@code x = x0} that is still "on the earth".
     *
     * @param x0 defines a line that intersects the earth, in the projection coordinate system.
     * @return the maximum y-coord along the line {@code x = x0} that is still "on the earth".
     * @throws IllegalArgumentException if the line {@code x = x0} does not intersect the earth.
     */
    public double calcMaxYAt(double x0) throws IllegalArgumentException {
        return getLimitYsAt(x0)[1];
    }

    /**
     * Calculates the minimum x-coord along the line {@code y = y0} that is still "on the earth".
     *
     * @param y0 defines a line that intersects the earth, in the projection coordinate system.
     * @return the minimum x-coord along the line {@code y = y0} that is still "on the earth".
     * @throws IllegalArgumentException if the line {@code y = y0} does not intersect the earth.
     */
    public double calcMinXAt(double y0) throws IllegalArgumentException {
        return getLimitXsAt(y0)[0];
    }

    /**
     * Calculates the maximum x-coord along the line {@code y = y0} that is still "on the earth".
     *
     * @param y0 defines a line that intersects the earth, in the projection coordinate system.
     * @return the maximum x-coord along the line {@code y = y0} that is still "on the earth".
     * @throws IllegalArgumentException if the line {@code y = y0} does not intersect the earth.
     */
    public double calcMaxXAt(double y0) throws IllegalArgumentException {
        return getLimitXsAt(y0)[1];
    }

    public List<ProjectionPoint> getMapEdgeInterceptsAtX(double x0) {
        List<ProjectionPoint> mapEdgeIntercepts = new LinkedList<>();
        if (projToLatLon(x0, falseNorthing) == INVALID) {  // The line {@code x = x0} does not intersect the earth.
            return mapEdgeIntercepts;  // Empty list.
        }

        double x0natural = x0 - falseEasting;
        double limitLon_r = (x0natural < 0) ? -PI : +PI;
        double deltaLon_r = limitLon_r - Math.toRadians(centMeridian);

        // This formula comes from solving 30-1 for phi, and then plugging it into 30-2. See Snyder, p 247.
        double minY = -earthRadius * Math.acos(x0natural / (earthRadius * deltaLon_r));
        double maxY = +earthRadius * Math.acos(x0natural / (earthRadius * deltaLon_r));

        mapEdgeIntercepts.add(new ProjectionPointImpl(x0, minY + falseNorthing));
        mapEdgeIntercepts.add(new ProjectionPointImpl(x0, maxY + falseNorthing));
        return mapEdgeIntercepts;
    }

    public List<ProjectionPoint> getMapEdgeInterceptsAtY(double y0) {
        List<ProjectionPoint> mapEdgeIntercepts = new LinkedList<>();
        if (projToLatLon(falseEasting, y0) == INVALID) {  // The line {@code y = y0} does not intersect the earth.
            return mapEdgeIntercepts;  // Empty list.
        }

        double minX = calcXat(y0, -PI);
        double maxX = calcXat(y0, +PI);

        mapEdgeIntercepts.add(new ProjectionPointImpl(minX, y0));
        mapEdgeIntercepts.add(new ProjectionPointImpl(maxX, y0));
        return mapEdgeIntercepts;
    }

    /**
     * Calculates the minimum and maximum y-coords along the line {@code x = x0} that are still "on the earth".
     *
     * @param x0  defines a line that intersects the earth, in the projection coordinate system.
     * @return the minimum and maximum y-coords along the line {@code x = x0} that are still "on the earth".
     * @throws IllegalArgumentException  if the line {@code x = x0} does not intersect the earth.
     */
    public double[] getLimitYsAt(double x0) throws IllegalArgumentException {
        if (projToLatLon(x0, falseNorthing) == INVALID) {
            throw new IllegalArgumentException("mapEdgeIntercepts. x0 = " + x0);
        }

        double x0natural = x0 - falseEasting;
        double limitLon_r = (x0natural < 0) ? -PI : +PI;
        double deltaLon_r = limitLon_r - Math.toRadians(centMeridian);

        // This formula comes from solving 30-1 for phi, and then plugging it into 30-2. See Snyder, p 247.
        double minY = -earthRadius * Math.acos(x0natural / (earthRadius * deltaLon_r));
        double maxY = +earthRadius * Math.acos(x0natural / (earthRadius * deltaLon_r));
        return new double[] { minY + falseNorthing, maxY + falseNorthing };
    }

    /**
     * Calculates the minimum and maximum x-coords along the line {@code y = y0} that are still "on the earth".
     *
     * @param y0 defines a line that intersects the earth, in the projection coordinate system.
     * @return the minimum and maximum x-coords along the line {@code y = y0} that are still "on the earth".
     * @throws IllegalArgumentException if the line {@code y = y0} does not intersect the earth.
     */
    public double[] getLimitXsAt(double y0) throws IllegalArgumentException {
        if (projToLatLon(falseEasting, y0) == INVALID) {
            throw new IllegalArgumentException("The line y = y0 does not intersect the earth. y0 = " + y0);
        }

        double minX = calcXat(y0, -PI);
        double maxX = calcXat(y0, +PI);

        return new double[] { minX, maxX };
    }

    private double calcXat(double y0, double lon_r) {
        double y0natural = y0 - falseNorthing;
        double deltaLon_r = lon_r - Math.toRadians(centMeridian);

        // This formula comes from plugging 30-6 into 30-1. See Snyder, p 247-248.
        double x = earthRadius * deltaLon_r * Math.cos(y0natural / earthRadius);

        return x + falseEasting;
    }

    private LatLonRect makeLatLonRect(List<ProjectionPoint> projPoints) {
        // FIXME: What is projPoints is empty? Can we return some kind of empty/invalid LatLonRect?

        double minLat = +Double.MAX_VALUE;
        double minLon = +Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (ProjectionPoint projPoint : projPoints) {
            LatLonPoint latLonPoint = projToLatLon(projPoint);
            assert latLonPoint != INVALID : "We should have filtered out bad points and added good ones. WTF?";

            minLat = Math.min(minLat, latLonPoint.getLatitude());
            minLon = Math.min(minLon, latLonPoint.getLongitude());
            maxLat = Math.max(maxLat, latLonPoint.getLatitude());
            maxLon = Math.max(maxLon, latLonPoint.getLongitude());
        }

        return new LatLonRect(new LatLonPointImpl(minLat, minLon), new LatLonPointImpl(maxLat, maxLon));
    }
}
