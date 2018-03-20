/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc;

import ucar.unidata.util.Parameter;

/**
 * Projective geometry transformations from (lat,lon) to (x,y) on
 * a projective cartesian surface.
 * @author John Caron
 */

public interface Projection {

  /**
   * The name of this class of projections, eg "Transverse Mercator".
   *
   * @return the class name
   */
  public String getClassName();

  /**
   * The name of this projection.
   *
   * @return the name of this projection
   */
  public String getName();

  /**
   * String representation of the projection parameters.
   *
   * @return String representation of the projection parameters.
   */
  public String paramsToString();

  /**
   * Convert a LatLonPoint to projection coordinates.  Note: do not assume
   * a new object is created on each call for the return value.
   *
   * @param latlon convert from these lat, lon coordinates
   * @param result point to put result in
   * @return ProjectionPoint convert to these projection coordinates
   */
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl result);

  /**
   * Convert projection coordinates to a LatLonPoint.
   * Note: do not assume a new object is created on each call for the
   * return value.
   *
   * @param ppt    convert from these projection coordinates
   * @param result return result here, or null
   * @return lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl result);

  /**
   * Does the line between these two points cross the projection "seam", which
   * is a discontinuity in the function latlon <-> projection plane
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
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
   * @param proj projection to check
   * @return true if this represents the same Projection as proj.
   */
  public boolean equals(Object proj);

  /**
   * Get parameters as list of ucar.unidata.util.Parameter
   *
   * @return List of parameters
   */
  public java.util.List<Parameter> getProjectionParameters();

}

