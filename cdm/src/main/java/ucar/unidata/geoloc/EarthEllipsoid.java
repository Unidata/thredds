/*
 * Copyright 2002 Unidata Program Center/University Corporation for
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

// $Id: EarthEllipsoid.java,v 1.5 2006/11/18 19:03:12 dmurray Exp $


package ucar.unidata.geoloc;


import java.util.ArrayList;
import java.util.Collection;


import java.util.List;


/**
 * Type-safe enumeration of Earth Ellipsoids. Follows EPSG.
 *
 * @author john caron
 * @version $Revision: 1.5 $ $Date: 2006/11/18 19:03:12 $
 *
 * @see "http://www.epsg.org/"
 */

public final class EarthEllipsoid extends Earth {

    // predefined ellipsoids

    /** _more_ */
    public final static EarthEllipsoid WGS84 = new EarthEllipsoid("WGS84",
                                                   7030, 6378137.0,
                                                   299.1528128);

    /** _more_ */
    private static java.util.LinkedHashMap hash;

    /** _more_ */
    private static java.util.ArrayList list;

    /** _more_ */
    private String name;

    /** _more_ */
    private int epsgId;

    /**
     * Constructor.
     * @param name EPSG name
     * @param epsgId EPSG id
     * @param a semimajor (equatorial) radius, in meters.
     * @param f inverse flattening.
     */
    public EarthEllipsoid(String name, int epsgId, double a, double f) {
        super(a, f, true);
        this.name   = name;
        this.epsgId = epsgId;
        if (hash == null) {
            hash = new java.util.LinkedHashMap(10);
        }
        hash.put(name, this);
        getAll().add(this);
    }

    /**
     * get a collection of all defined EarthEllipsoid objects
     *
     * @return _more_
     */
    public static Collection getAll() {
        if (list == null) {
            list = new ArrayList();
        }
        return list;
    }





    /**
     * Find the EarthEllipsoid that matches this name.
     *
     * @param name : name to match
     * @return EarthEllipsoid or null if no match.
     */
    public static EarthEllipsoid getType(String name) {
        if (name == null) {
            return null;
        }
        if (hash == null) {
            hash = new java.util.LinkedHashMap(10);
        }
        return (EarthEllipsoid) hash.get(name);
    }

    /**
     * Find the EarthEllipsoid that matches this EPSG Id.
     *
     * @param epsgId : epsg Id to match
     * @return EarthEllipsoid or null if no match.
     */
    public static EarthEllipsoid getType(int epsgId) {
        List list = (List) getAll();
        for (int i = 0; i < list.size(); i++) {
            EarthEllipsoid ellipsoid = (EarthEllipsoid) list.get(i);
            if (ellipsoid.epsgId == epsgId) {
                return ellipsoid;
            }
        }
        return null;
    }

    /**
     * EPSG name
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * EPSG id
     *
     * @return _more_
     */
    public int getEpsgId() {
        return epsgId;
    }

    /**
     * Same as EPSG name
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * Override Object.hashCode() to be consistent with this equals.
     *
     * @return _more_
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Objects with same name are equal.
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ( !(o instanceof EarthEllipsoid)) {
            return false;
        }
        return o.hashCode() == this.hashCode();
    }
}

/**
 * $Log: EarthEllipsoid.java,v $
 * Revision 1.5  2006/11/18 19:03:12  dmurray
 * jindent
 *
 * Revision 1.4  2005/08/11 22:42:11  dmurray
 * jindent (I'll leave the javadoc to those who forgot to)
 *
 * Revision 1.3  2005/05/13 11:14:08  jeffmc
 * Snapshot
 *
 * Revision 1.2  2005/05/06 14:01:55  dmurray
 * run jindent on these.  It would be good if we could agree on using
 * Jindent for the common packages.
 *
 * Revision 1.1  2005/02/01 01:35:51  caron
 * no message
 *
 */

