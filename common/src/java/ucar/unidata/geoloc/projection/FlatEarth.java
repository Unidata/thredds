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


package ucar.unidata.geoloc.projection;


import ucar.unidata.geoloc.*;



/**
 *   FlatEarth Projection
 *   This projection surface is tangent at some point (lat0, lon0) and
 *   has a y axis rotated from true North by some angle.
 *
 *   We call it "flat" because it should only be used where the spherical
 *   geometry of the earth is not significant. In actuallity, we use the simple
 *   "arclen" routine which computes dy along a meridian, and dx along a
 *   latitude circle.  We rotate the coordinate system to/from a true north system.
 *  <p>
 *  See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 *  2nd edition (1983), p 145
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author Unidata Development Team
 */

public class FlatEarth extends ProjectionImpl {

    /* double Lat0, Lon0, Rot_cos, Rot_sin;*/

    /** constants from Snyder's equations */
    private double rotAngle;

    /** center lat/lon in radians */
    private double lat0, lon0;

    /** some constants */
    private double cosRot, sinRot;

    /** origin */
    private LatLonPointImpl origin;  // why are we keeping this?

    /** spherical vs ellipsoidal */
    private boolean spherical = false;

    private double radius = Earth.getRadius() * .001; // km

      /** copy constructor - avoid clone !! */
    public ProjectionImpl constructCopy() {
      return new FlatEarth( getOriginLat(), getOriginLon(), getRotationAngle());     
    }

    /**
     *  Constructor with default parameters
     */
    public FlatEarth() {
        this(0.0, 0.0, 0.0);
    }


    /**
     * Construct a FlatEarth Projection, two standard parellels.
     * For the one standard parellel case, set them both to the same value.
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     * @param rotAngle   angle of rotation, in degrees
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public FlatEarth(double lat0, double lon0, double rotAngle) {

        this.lat0     = Math.toRadians(lat0);
        this.lon0     = Math.toRadians(lon0);
        this.rotAngle = Math.toRadians(rotAngle);

        origin        = new LatLonPointImpl(lat0, lon0);
        precalculate();

        addParameter(ATTR_NAME, "flat_earth");
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("longitude_of_projection_origin", lon0);
        addParameter("rotationAngle", rotAngle);
    }

    /**
     * Construct a FlatEarth Projection, two standard parellels.
     * For the one standard parellel case, set them both to the same value.
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     *  rotAngle angle of rotation is default to 0.0
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public FlatEarth(double lat0, double lon0) {

        this.lat0     = Math.toRadians(lat0);
        this.lon0     = Math.toRadians(lon0);
        this.rotAngle = Math.toRadians(0.0);

        origin        = new LatLonPointImpl(lat0, lon0);
        precalculate();

        addParameter(ATTR_NAME, "flat_earth");
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("longitude_of_projection_origin", lon0);
        addParameter("rotationAngle", rotAngle);
        // addParameter("units", "km");
    }
    // move this to ucar.unit or ucar.unidata.util

    /**
     * Precalculate some stuff
     */
    private void precalculate() {
        sinRot = Math.sin(rotAngle);
        cosRot = Math.cos(rotAngle);
    }

    /**
     * Clone this projection.
     *
     * @return Clone of this
     */
    public Object clone() {
        FlatEarth cl = (FlatEarth) super.clone();
        cl.origin = new LatLonPointImpl(getOriginLat(), getOriginLon());
        return cl;
    }


    /**
     * Check for equality with the Object in question
     *
     * @param proj  object to check
     * @return true if they are equal
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof FlatEarth)) {
            return false;
        }

        FlatEarth oo = (FlatEarth) proj;
        return ((this.getOriginLat() == oo.getOriginLat())
                && (this.getOriginLon() == oo.getOriginLon())
                && (this.rotAngle == oo.rotAngle));
    }


    // bean properties

    /**
     * Get the origin longitude.
     * @return the origin longitude.
     */
    public double getOriginLon() {
        return origin.getLongitude();
    }

    /**
     * Set the origin longitude.
     * @param lon   the origin longitude.
     */
    public void setOriginLon(double lon) {
        origin.setLongitude(lon);
        lon0 = Math.toRadians(lon);
        precalculate();
    }

  /**
    * Get the origin latitude.
    * @return the origin latitude.
    */
   public double getOriginLat() {
       return origin.getLatitude();
   }

  /**
    * Get the rotation angle.
    * @return the origin latitude.
    */
   public double getRotationAngle() {
       return rotAngle;
   }

   /**
    * Set the origin latitude.
    *
    * @param lat   the origin latitude.
    */
   public void setOriginLat(double lat) {
       origin.setLatitude(lat);
       lat0 = Math.toRadians(lat);
       precalculate();
   }

     /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "FlatEarth";
    }

    /**
     * Create a String of the parameters.
     * @return a String of the parameters
     */
    public String paramsToString() {
        return " origin " + origin.toString() + " rotationAngle " + rotAngle;
    }



    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                        ProjectionPointImpl result) {
        double toX, toY;
        double fromLat = latLon.getLatitude();
        double fromLon = latLon.getLongitude();
        double dx, dy;

        fromLat = Math.toRadians(fromLat);

        dy      = radius * (fromLat - lat0);
        dx = radius * Math.cos(fromLat)
             * (Math.toRadians(fromLon) - lon0);


        toX = cosRot * dx - sinRot * dy;
        toY = sinRot * dx + cosRot * dy;


        result.setLocation(toX, toY);

        return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */

    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        double toLat, toLon;
        double x = world.getX();
        double y = world.getY();
        double cosl;
        int    TOLERENCE = 1;
        double xp, yp;

        xp    = cosRot * x + sinRot * y;
        yp    = -sinRot * x + cosRot * y;

        toLat = Math.toDegrees(lat0) + Math.toDegrees(yp / radius);
        //double lat2;
        //lat2 = lat0 + Math.toDegrees(yp/radius);
        cosl = Math.cos(Math.toRadians(toLat));
        if (Math.abs(cosl) < TOLERANCE) {
            toLon = Math.toDegrees(lon0);
        } else {
            toLon = Math.toDegrees(lon0)
                    + Math.toDegrees(xp / cosl / radius);
        }

        toLon = LatLonPointImpl.lonNormal(toLon);

        result.setLatitude(toLat);
        result.setLongitude(toLon);
        return result;
    }



    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates,
     *                 where to[0][i], to[1][i] is the (x,y) coordinate
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */

    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                  int lonIndex) {
        int     cnt      = from[0].length;
        float[] fromLatA = from[latIndex];
        float[] fromLonA = from[lonIndex];
        float[] resultXA = to[INDEX_X];
        float[] resultYA = to[INDEX_Y];
        double  toX, toY;

        for (int i = 0; i < cnt; i++) {
            double fromLat = fromLatA[i];
            double fromLon = fromLonA[i];

            fromLat = Math.toRadians(fromLat);
            double dy = radius * (fromLat - lat0);
            double dx = radius * Math.cos(fromLat)
                        * (Math.toRadians(fromLon) - lon0);


            toX         = cosRot * dx - sinRot * dy;
            toY         = sinRot * dx + cosRot * dy;

            resultXA[i] = (float) toX;
            resultYA[i] = (float) toY;
        }
        return to;
    }

    /**
     *  This returns true when the line between pt1 and pt2 crosses the seam.
     *  When the cone is flattened, the "seam" is lon0 +- 180.
     *
     * @param pt1   point 1
     * @param pt2   point 2
     * @return true when the line between pt1 and pt2 crosses the seam.
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {

        return (pt1.getX() * pt2.getX() < 0);
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */

    public float[][] projToLatLon(float[][] from, float[][] to) {
        int     cnt    = from[0].length;
        float[] fromXA = from[INDEX_X];
        float[] fromYA = from[INDEX_Y];
        float[] toLatA = to[INDEX_LAT];
        float[] toLonA = to[INDEX_LON];

        double  toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];

            double xp    = cosRot * fromX + sinRot * fromY;
            double yp    = -sinRot * fromX + cosRot * fromY;


            toLat = Math.toDegrees(lat0)
                    + Math.toDegrees(yp / radius);
            double cosl = Math.cos(Math.toRadians(toLat));

            if (Math.abs(cosl) < TOLERANCE) {
                toLon = Math.toDegrees(lon0);
            } else {
                toLon = Math.toDegrees(lon0)
                        + Math.toDegrees(xp / cosl / radius);
            }

            toLon     = LatLonPointImpl.lonNormal(toLon);

            toLatA[i] = (float) toLat;
            toLonA[i] = (float) toLon;
        }
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates,
     *                 where to[0][i], to[1][i] is the (x,y) coordinate
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */

    public double[][] latLonToProj(double[][] from, double[][] to,
                                   int latIndex, int lonIndex) {
        int      cnt      = from[0].length;
        double[] fromLatA = from[latIndex];
        double[] fromLonA = from[lonIndex];
        double[] resultXA = to[INDEX_X];
        double[] resultYA = to[INDEX_Y];
        double   toX, toY;

        for (int i = 0; i < cnt; i++) {
            double fromLat = fromLatA[i];
            double fromLon = fromLonA[i];

            fromLat = Math.toRadians(fromLat);
            double dy = radius * (fromLat - lat0);
            double dx = radius * Math.cos(fromLat)
                        * (Math.toRadians(fromLon) - lon0);

            toX         = cosRot * dx - sinRot * dy;
            toY         = sinRot * dx + cosRot * dy;


            resultXA[i] = toX;
            resultYA[i] = toY;
        }
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */

    public double[][] projToLatLon(double[][] from, double[][] to) {
        int      cnt    = from[0].length;
        double[] fromXA = from[INDEX_X];
        double[] fromYA = from[INDEX_Y];
        double[] toLatA = to[INDEX_LAT];
        double[] toLonA = to[INDEX_LON];

        double   toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];

            double xp    = cosRot * fromX + sinRot * fromY;
            double yp    = -sinRot * fromX + cosRot * fromY;

            //toLat =  lat0 + Math.toDegrees(yp);
            toLat = Math.toDegrees(lat0)
                    + Math.toDegrees(yp / radius);
            double cosl = Math.cos(Math.toRadians(toLat));

            if (Math.abs(cosl) < TOLERANCE) {
                toLon = Math.toDegrees(lon0);
            } else {
                toLon = Math.toDegrees(lon0)
                        + Math.toDegrees(xp / cosl / radius);
            }

            toLon     = LatLonPointImpl.lonNormal(toLon);

            toLatA[i] = toLat;
            toLonA[i] = toLon;

        }
        return to;
    }

    /*ENDGENERATED*/

    /**
     * Test
     *
     * @param args not used
     */
    public static void main(String[] args) {
        FlatEarth           a = new FlatEarth(90, -100, 0.0);
        ProjectionPointImpl p = a.latLonToProj(89, -101);
        System.out.println("proj point = " + p);
        LatLonPoint ll = a.projToLatLon(p);
        System.out.println("ll = " + ll);
    }

}

