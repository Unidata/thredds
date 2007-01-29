/*
 * $Id: Projection.java,v 1.20 2006/11/18 19:03:13 dmurray Exp $
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
 * Projective geometry transformations from (lat,lon) to (x,y) on
 *  a projective cartesian surface. We use the java.awt.geom 2D classes
 *  to represent the coordinates on the projective plane.
 *
 * @author John Caron
 * @version $Id: Projection.java,v 1.20 2006/11/18 19:03:13 dmurray Exp $
 */

public interface Projection {

    /**
     * The name of this class of projections, eg "Transverse Mercator".
     * @return the class name
     */
    public String getClassName();

    /**
     * The name of this projection.
     * @return the name of this projection
     */
    public String getName();

    /**
     * String representation of the projection parameters.
     * @return String representation of the projection parameters.
     */
    public String paramsToString();

    /**
     * Convert a LatLonPoint to projection coordinates.  Note: do not assume
     * a new object is created on each call for the return value.
     *
     * @param latlon convert from these lat, lon coordinates
     * @param result  point to put result in
     *
     * @return ProjectionPoint convert to these projection coordinates
     */
    public ProjectionPoint latLonToProj(LatLonPoint latlon,
                                        ProjectionPointImpl result);

    /**
     * Convert projection coordinates to a LatLonPoint.
     * Note: do not assume a new object is created on each call for the
     * return value.
     * @param ppt convert from these projection coordinates
     * @param result return result here, or null
     *
     * @return lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint ppt,
                                    LatLonPointImpl result);

    /**
     * Does the line between these two points cross the projection "seam", which
     * is a discontinuity in the function latlon <-> projection plane
     *
     * @param pt1  the line goes between these two points
     * @param pt2  the line goes between these two points
     *
     * @return false if there is no seam, or the line does not cross it.
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2);

    /**
     * Get a reasonable bounding box in this projection.
     * Projections are typically specific to an area of the world;
     * theres no bounding box that works for all projections.
     *
     * @return a reasonable bounding box in this projection.
     */
    public ProjectionRect getDefaultMapArea();

    /**
     * Check for equality with the object in question
     *
     * @param proj  projection to check
     *
     * @return true if this represents the same Projection as proj.
     */
    public boolean equals(Object proj);

    /**
     * Get parameters as list of ucar.unidata.util.Parameter
     * @return List of parameters
     */
    public java.util.List getProjectionParameters();

}

