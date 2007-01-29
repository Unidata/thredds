/*
 * $Id: LatLonRect.java,v 1.15 2006/11/18 19:03:13 dmurray Exp $
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



/**
 * Bounding box for latitude/longitude points. This is a rectangle
 * in lat/lon coordinates.
 * This class handles the longitude wrapping problem.
 * Note that LatLonPoint always has lon in the range +/-180.
 *
 * @author Russ Rew
 * @author John Caron
 * @version $Id: LatLonRect.java,v 1.15 2006/11/18 19:03:13 dmurray Exp $
 */
public class LatLonRect {

    /** upper right corner */
    private LatLonPointImpl upperRight;

    /** lower left corner */
    private LatLonPointImpl lowerLeft;

    /** flag for dateline cross */
    private boolean crossDateline = false;

    /** width and initial longitude */
    private double width, lon0;

    /**
     * Construct a lat/lon bounding box from a point, and a delta lat, lon.
     * This disambiguates which way the box wraps around the globe.
     *
     * @param p1 one corner of the box
     * @param deltaLat delta lat from p1. (may be positive or negetive)
     * @param deltaLon delta lon from p1. (may be positive or negetive)
     */
    public LatLonRect(LatLonPoint p1, double deltaLat, double deltaLon) {
        double lonmin, lonmax;
        double latmin = Math.min(p1.getLatitude(),
                                 p1.getLatitude() + deltaLat);
        double latmax = Math.max(p1.getLatitude(),
                                 p1.getLatitude() + deltaLat);

        double lonpt = p1.getLongitude();
        if (deltaLon > 0) {
            lonmin        = lonpt;
            lonmax        = lonpt + deltaLon;
            crossDateline = (lonmax > 180.0);
        } else {
            lonmax        = lonpt;
            lonmin        = lonpt + deltaLon;
            crossDateline = (lonmin < -180.0);
        }

        this.lowerLeft  = new LatLonPointImpl(latmin, lonmin);
        this.upperRight = new LatLonPointImpl(latmax, lonmax);

        // these are an alternative way to view the longitude range
        this.width = Math.abs(deltaLon);
        this.lon0 = LatLonPointImpl.lonNormal(p1.getLongitude()
                + deltaLon / 2);
    }

    /**
     * Construct a lat/lon bounding box from two points.
     * The order of longitude coord of the two points matters:
     *   pt1.lon is always the "left" point, then points contained within the box
     *   increase (unless crossing the Dateline, in which case they jump to -180, but
     *  then start increasing again) until pt2.lon
     * The order of lat doesnt matter: smaller will go to "lower" point (further south)
     *
     * @param left left corner
     * @param right right corner
     */
    public LatLonRect(LatLonPoint left, LatLonPoint right) {
        this(left, right.getLatitude() - left.getLatitude(),
             LatLonPointImpl.lonNormal360(right.getLongitude()
                                          - left.getLongitude()));
    }

    /**
     * Copy Constructor
     *
     * @param r  rectangle to copy
     */
    public LatLonRect(LatLonRect r) {
        this(r.getLowerLeftPoint(),
             r.getUpperRightPoint().getLatitude()
             - r.getLowerLeftPoint().getLatitude(), r.getWidth());
    }

    /**
     * Create a LatLonRect that covers the whole world.
     */
    public LatLonRect() {
        this(new LatLonPointImpl(-90, -180), 180, 360);
    }

    /**
     * Get the upper right corner of the bounding box.
     * @return upper right corner of the bounding box
     */
    public LatLonPointImpl getUpperRightPoint() {
        return upperRight;
    }

    /**
     * Get the lower left corner of the bounding box.
     * @return lower left corner of the bounding box
     */
    public LatLonPointImpl getLowerLeftPoint() {
        return lowerLeft;
    }

    /**
     * Get the upper left corner of the bounding box.
     * @return upper left corner of the bounding box
     */
    public LatLonPointImpl getUpperLeftPoint() {
        return new LatLonPointImpl(upperRight.getLatitude(),
                                   lowerLeft.getLongitude());
    }

    /**
     * Get the lower left corner of the bounding box.
     * @return lower left corner of the bounding box
     */
    public LatLonPointImpl getLowerRightPoint() {
        return new LatLonPointImpl(lowerLeft.getLatitude(),
                                   upperRight.getLongitude());
    }





    /**
     * Get whether the bounding box crosses the +/- 180 seam
     * @return true if the bounding box crosses the +/- 180 seam
     */
    public boolean crossDateline() {
        return crossDateline;
    }

    /**
     * get whether two bounding boxes are equal in values
     * @param other other bounding box
     * @return true if this represents the same bounding box as other
     */
    public boolean equals(LatLonRect other) {
        return lowerLeft.equals(other.getLowerLeftPoint())
               && upperRight.equals(other.getUpperRightPoint());
    }

    /**
     * return width of bounding box, always between 0 and 360 degrees.
     * @return width of bounding box in degrees longitude
     */
    public double getWidth() {
        return width;
    }

    /**
     * return center Longitude, always in the range +/-180
     * @return center Longitude
     */
    public double getCenterLon() {
        return lon0;
    }


    /**
     * Determine if a specified LatLonPoint is contained in this bounding box.
     * @param p the specified point to be tested
     * @return true if point is contained in this bounding box
     */
    public boolean contains(LatLonPoint p) {
        return contains(p.getLatitude(), p.getLongitude());
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public boolean contains(double lat, double lon) {
        // check lat first
        if ((lat < lowerLeft.getLatitude())
                || (lat > upperRight.getLatitude())) {
            return false;
        }

        if (crossDateline) {
            // bounding box crosses the +/- 180 seam
            return ((lon >= lowerLeft.getLongitude())
                    || (lon <= upperRight.getLongitude()));
        } else {
            // check "normal" lon case
            return ((lon >= lowerLeft.getLongitude())
                    && (lon <= upperRight.getLongitude()));
        }
    }


    /**
     * Determine if this bounding box is contained in another LatLonRect.
     * @param b the other box to see if it contains this one
     * @return true if b contained in this bounding box
     */
    public boolean containedIn(LatLonRect b) {
        return (b.getWidth() >= width) && b.contains(upperRight)
               && b.contains(lowerLeft);
    }


    /*
     * Determine if a specified LatLonRect intersects this
     * @param b the specified box to be tested
     *
     * @param p
     * @return true if b intersects this bounding box
     *
     * public boolean intersects(LatLonRect b) {
     *     if (b.getUpperRightPoint().getLatitude() < lowerLeft.getLatitude())
     * return false;
     *     if (b.getLowerLeftPoint().getLatitude() > upperRight.getLatitude())
     * return false;
     *
     * double blon0 = b.getCenterLon();
     * double normal = (blon0 + lon0) / 2;
     * if (Math.abs(blon0-lon0) > 180.0)
     * normal += 180;
     * blon0 = LatLonPoint.lonNormal(blon0, normal);
     * double mylon0 = LatLonPoint.lonNormal(lon0, normal);
     *
     *     if (blon0 + b.getWidth() < mylon0 - width)
     * return false;
     *     if (blon0 - b.getWidth() > mylon0 + width)
     * return false;
     *
     * return true;
     * }
     */

    /**
     * Extend the bounding box to contain this point
     *
     * @param p  point to include
     */
    public void extend(LatLonPoint p) {
        if (contains(p)) {
            return;
        }

        double lat = p.getLatitude();
        double lon = p.getLongitude();

        // lat is easy to deal with
        if (lat > upperRight.getLatitude()) {
            upperRight.setLatitude(lat);
        }
        if (lat < lowerLeft.getLatitude()) {
            lowerLeft.setLatitude(lat);
        }

        // lon is uglier
        if (crossDateline) {

            // bounding box crosses the +/- 180 seam
            double d1 = lon - upperRight.getLongitude();
            double d2 = lowerLeft.getLongitude() - lon;
            if ((d1 > 0.0) && (d2 > 0.0)) {  // needed ?
                if (d1 > d2) {
                    lowerLeft.setLongitude(lon);
                } else {
                    upperRight.setLongitude(lon);
                }
            }

        } else {

            // normal case
            if (lon > upperRight.getLongitude()) {
                if (lon - upperRight.getLongitude()
                        > lowerLeft.getLongitude() - lon + 360) {
                    crossDateline = true;
                    lowerLeft.setLongitude(lon);
                } else {
                    upperRight.setLongitude(lon);
                }
            } else if (lon < lowerLeft.getLongitude()) {
                if (lowerLeft.getLongitude() - lon
                        > lon + 360.0 - upperRight.getLongitude()) {
                    crossDateline = true;
                    upperRight.setLongitude(lon);
                } else {
                    lowerLeft.setLongitude(lon);
                }
            }
        }

        // recalc delta, center
        width = upperRight.getLongitude() - lowerLeft.getLongitude();
        lon0  = (upperRight.getLongitude() + lowerLeft.getLongitude()) / 2;
        if (crossDateline) {
            width += 360;
            lon0  -= 180;
        }
    }

    /**
     * Extend the bounding box to contain the given rectanle
     *
     * @param r  rectangle to include
     */
    public void extend(LatLonRect r) {
        extend(r.getLowerLeftPoint());
        extend(r.getUpperRightPoint());
    }


    /**
     * Return a String representation of this object.
     *
     * @return a String representation of this object.
     */
    public String toString() {
        return " ll: " + lowerLeft + "+ ur: " + upperRight;
    }
}

/*
 *  Change History:
 *  $Log: LatLonRect.java,v $
 *  Revision 1.15  2006/11/18 19:03:13  dmurray
 *  jindent
 *
 *  Revision 1.14  2006/07/19 21:10:34  jeffmc
 *  New contains method
 *
 *  Revision 1.13  2006/04/01 02:30:22  caron
 *  netcdf Server
 *
 *  Revision 1.12  2005/11/07 23:14:51  jeffmc
 *  add getUpperLeft/getLowerRight utilities
 *
 *  Revision 1.11  2005/05/13 18:29:09  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.10  2005/04/26 22:28:50  dmurray
 *  add in a no arg ctor that covers the whole world
 *
 *  Revision 1.9  2004/09/22 21:22:58  caron
 *  mremove nc2 dependence
 *
 *  Revision 1.8  2004/07/30 16:24:40  dmurray
 *  Jindent and javadoc
 *
 *  Revision 1.7  2004/06/07 20:22:50  caron
 *  javadoc
 *
 *  Revision 1.6  2004/02/27 21:21:27  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:34:57  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2003/10/28 18:21:00  caron
 *  add copy constructor, extend()
 *
 *  Revision 1.3  2003/04/08 15:59:06  caron
 *  rework for nc2 framework
 *
 *  Revision 1.1  2002/12/13 00:53:09  caron
 *  pass 2
 *
 *  Revision 1.1.1.1  2002/02/26 17:24:45  caron
 *  import sources
 *
 *  Revision 1.2  2000/08/18 04:15:16  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.1  1999/12/16 22:57:21  caron
 *  gridded data viewer checkin
 *
 *  Revision 1.3  1999/06/03 01:43:49  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:26:14  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:51  caron
 *  startAgain
 *
 * # Revision 1.4  1999/03/16  16:56:56  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.3  1999/03/03  19:58:09  caron
 * # more java2D changes
 * #
 * # Revision 1.2  1998/12/14  17:10:46  russ
 * # Add comment for accumulating change histories.
 * #
 */









