/*
 * $Id:LatLonPointImpl.java 63 2006-07-12 21:50:51Z edavis $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.geoloc;


import ucar.unidata.util.Format;


/**
 * Our implementation of LatLonPoint.
 * Longitude is always between -180 and +180 deg.
 * Latitude is always between -90 and +90 deg.
 *
 * @see LatLonPoint
 * @author Russ Rew
 * @author John Caron
 * @version $Id:LatLonPointImpl.java 63 2006-07-12 21:50:51Z edavis $
 */
public class LatLonPointImpl implements LatLonPoint, java.io.Serializable {

    /**
     * Test if point lies between two longitudes, deal with wrapping.
     * @param lon point to test
     * @param lonBeg beginning longitude
     * @param lonEnd ending longitude
     * @return true if lon is between lonBeg and lonEnd.
     */
    static public boolean betweenLon(double lon, double lonBeg,
                                     double lonEnd) {
        lonBeg = lonNormal(lonBeg, lon);
        lonEnd = lonNormal(lonEnd, lon);
        return (lon >= lonBeg) && (lon <= lonEnd);
    }

    /**
     * put longitude into the range [-180, 180] deg
     *
     * @param lon  lon to normalize
     * @return longitude in range [-180, 180] deg
     */
    static public final double range180(double lon) {
        return lonNormal(lon);
    }

    /**
     * put longitude into the range [0, 360] deg
     *
     * @param lon  lon to normalize
     * @return longitude into the range [0, 360] deg
     */
    static public final double lonNormal360(double lon) {
        return lonNormal(lon, 180.0);
    }

    /**
     * put longitude into the range [center +/- 180] deg
     *
     * @param lon  lon to normalize
     * @param center  center point
     * @return longitude into the range [center +/- 180] deg
     */
    static public final double lonNormal(double lon, double center) {
        return center + Math.IEEEremainder(lon - center, 360.0);
    }

    /**
     * Normalize the longitude to lie between +/-180
     * @param lon east latitude in degrees
     * @return normalized lon
     */
    static public double lonNormal(double lon) {
        if ((lon < -180.) || (lon > 180.)) {
            return Math.IEEEremainder(lon, 360.0);
        } else {
            return lon;
        }
    }

    /**
     * Normalize the latitude to lie between +/-90
     * @param lat north latitude in degrees
     * @return normalized lat
     */
    static public double latNormal(double lat) {
        if (lat < -90.) {
            return -90.;
        } else if (lat > 90.) {
            return 90.;
        } else {
            return lat;
        }
    }

    /** string buffer for latitude values */
    static StringBuffer latBuff = new StringBuffer(20);

    /**
     * Make a nicely formatted representation of a latitude, eg 40.34N or 12.9S.
     * @param lat the latitude.
     * @param sigDigits numer of significant digits to display.
     * @return String representation.
     */
    static public String latToString(double lat, int sigDigits) {
        boolean is_north = (lat >= 0.0);
        if ( !is_north) {
            lat = -lat;
        }

        latBuff.setLength(0);
        latBuff.append(Format.d(lat, sigDigits));
        latBuff.append(is_north
                       ? "N"
                       : "S");

        return latBuff.toString();
    }

    /** tring buffer for longitude values */
    static StringBuffer lonBuff = new StringBuffer(20);

    /**
     * Make a nicely formatted representation of a longitude, eg 120.3W or 99.99E.
     * @param lon the longitude.
     * @param sigDigits numer of significant digits to display.
     * @return String representation.
     */
    static public String lonToString(double lon, int sigDigits) {
        double  wlon    = lonNormal(lon);
        boolean is_east = (wlon >= 0.0);
        if ( !is_east) {
            wlon = -wlon;
        }

        lonBuff.setLength(0);
        lonBuff.append(Format.d(wlon, sigDigits));
        lonBuff.append(is_east
                       ? "E"
                       : "W");

        return lonBuff.toString();
    }


    ///////////////////////////////////////////////////////////////////////////////////

    /** East latitude in degrees, always +/- 90 */
    private double lat;

    /** North longitude in degrees, always +/- 180 */
    private double lon;

    /** Default constructor with values 0,0. */
    public LatLonPointImpl() {
        this(0.0, 0.0);
    }

    /**
     * Copy Constructor.
     *
     * @param pt  point to copy
     */
    public LatLonPointImpl(LatLonPoint pt) {
        this(pt.getLatitude(), pt.getLongitude());
    }

    /**
     * Creates a LatLonPoint from component latitude and longitude values.
     * The longitude is adjusted to be in the range [-180.,180.].
     *
     * @param lat north latitude in degrees
     * @param lon east longitude in degrees
     */
    public LatLonPointImpl(double lat, double lon) {
        setLatitude(lat);
        setLongitude(lon);
    }

    /**
     * Returns the longitude, in degrees.
     * @return the longitude, in degrees
     */
    public double getLongitude() {
        return lon;
    }

    /**
     * Returns the latitude, in degrees.
     * @return the latitude, in degrees
     */
    public double getLatitude() {
        return lat;
    }

    /**
     * set lat, lon using values of pt
     *
     * @param pt  point to use
     */
    public void set(LatLonPoint pt) {
        setLongitude(pt.getLongitude());
        setLatitude(pt.getLatitude());
    }

    /**
     * set lat, lon using double values
     *
     * @param lat  lat value
     * @param lon  lon value
     */
    public void set(double lat, double lon) {
        setLongitude(lon);
        setLatitude(lat);
    }

    /**
     * set lat, lon using float values
     *
     * @param lat  lat value
     * @param lon  lon value
     */
    public void set(float lat, float lon) {
        setLongitude((double) lon);
        setLatitude((double) lat);
    }

    /**
     *  Set the longitude, in degrees. It is normalized to +/-180.
     *
     *  @param lon east longitude in degrees
     */
    public void setLongitude(double lon) {
        this.lon = lonNormal(lon);
    }

    /**
     * Set the latitude, in degrees. Must lie beween +/-90
     *
     * @param lat north latitude in degrees
     */
    public void setLatitude(double lat) {
        this.lat = latNormal(lat);
    }



    /**
     * Check for equality with another object.
     *
     * @param obj object to check
     * @return true if this represents the same point as pt
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof LatLonPointImpl)) {
            return false;
        }
        LatLonPointImpl that = (LatLonPointImpl) obj;
        return (this.lat == that.lat) && (this.lon == that.lon);
    }

    /**
     * Check for equality with another point.
     *
     * @param pt   point to check
     * @return true if this represents the same point as pt
     */

    public boolean equals(LatLonPoint pt) {
        boolean lonOk = closeEnough(pt.getLongitude(), this.lon);
        if ( !lonOk) {
            lonOk = closeEnough(lonNormal360(pt.getLongitude()),
                                lonNormal360(this.lon));
        }

        return lonOk && closeEnough(pt.getLatitude(), this.lat);
    }


    /**
     * Check to see if the values are close enough.
     *
     * @param d1  first value
     * @param d2  second value
     *
     * @return true if they are pretty close
     */
    private boolean closeEnough(double d1, double d2) {
        // TODO:  This should be moved to a utility method in ucar.util
        // that all ucar classes could use.
        if (d1 != 0.0) {
            return Math.abs((d1 - d2) / d1) < 1.0e-9;
        }
        if (d2 != 0.0) {
            return Math.abs((d1 - d2) / d2) < 1.0e-9;
        }
        return true;
    }

    /**
     * Default string representation
     *
     * @return string representing this point
     */
    public String toString() {
        return toString(4);
    }

    /**
     * String representation in the form, eg 40.23N 105.1W
     *
     * @param sigDigits significant digits
     * @return  String representation
     */
    public String toString(int sigDigits) {
        StringBuffer sbuff = new StringBuffer(40);
        sbuff.setLength(0);
        sbuff.append(latToString(lat, sigDigits));
        sbuff.append(" ");
        sbuff.append(lonToString(lon, sigDigits));
        return sbuff.toString();
    }

}

/* Change History:
   $Log: LatLonPointImpl.java,v $
   Revision 1.22  2006/01/25 18:05:17  jeffmc
   Add a equals(object) method

   Revision 1.21  2005/05/27 00:33:42  caron
   remove StringBufffer object variable - no need to optimize gc anymore

   Revision 1.20  2005/05/13 18:29:09  jeffmc
   Clean up the odd copyright symbols

   Revision 1.19  2005/05/06 14:01:55  dmurray
   run jindent on these.  It would be good if we could agree on using
   Jindent for the common packages.

   Revision 1.18  2005/02/19 22:17:47  caron
   no message

   Revision 1.17  2004/12/10 15:07:50  dmurray
   Jindent John's changes

   Revision 1.16  2004/09/22 21:22:58  caron
   mremove nc2 dependence

   Revision 1.15  2004/07/30 16:24:40  dmurray
   Jindent and javadoc

   Revision 1.14  2004/06/07 20:17:42  caron
   use approx float compare

   Revision 1.13  2004/02/27 21:21:27  jeffmc
   Lots of javadoc warning fixes

   Revision 1.12  2004/01/29 17:34:57  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.11  2003/06/03 20:06:17  caron
   fix javadocs

   Revision 1.10  2003/04/15 16:03:05  jeffmc
   Lots of changes. Mostly added in a MACROBDY tag in the leaf proj classes
   so we only have to define the actual math once. The script convert.tcl
   is used to generate the different projToLatLon/latLonToProj methods.

   Cleaned up the use of hard coded indices into the latlon and proj arrays.

   Added utility attribute setting methods in ProjectionImpl for the leaf classes
   to use.

   The old way of using the workP and workL objects was not thread safe, there
   was no synchronization on those objects. Changed the leaf methods
   to take a workP and workL object that can be created by the parent class method.
   For individual conversion calls it is still no thread safe but for the bulk
   ones it is.

   Revision 1.9  2003/04/08 15:59:06  caron
   rework for nc2 framework

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:45  caron
   import sources

   Revision 1.4  2000/08/18 04:15:16  russ
   Licensed under GNU LGPL.

   Revision 1.3  2000/05/16 22:24:56  caron
   add latToString, LonToString

   Revision 1.2  2000/05/09 20:42:49  caron
   change deprecated Format method

   Revision 1.1  1999/12/16 23:06:56  caron
   projection changes

   Revision 1.6  1999/07/07 19:37:08  dmurray
   setLat/setLon vs setLatitude/setLongitude changes

   Revision 1.5  1999/07/07 19:33:22  dmurray
   more changes for setLat, setLon deprecation and setLatitude and setLongitude
   additions

   Revision 1.4  1999/07/07 19:18:38  dmurray
   deprecated setLat, setLon, added setLatitude, setLongitude to go with
   corresponding get methods

   Revision 1.3  1999/06/03 01:43:49  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:15  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:52  caron
   startAgain

# Revision 1.5  1999/03/03  19:58:11  caron
# more java2D changes
#
# Revision 1.4  1999/02/15  23:05:17  caron
# upgrade to java2D, new ProjectionManager
#
# Revision 1.3  1998/12/14  17:10:47  russ
# Add comment for accumulating change histories.
#
*/







