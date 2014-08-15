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
 * Adapts a Projection interface into a subclass of
 * ProjectionImpl, so we can assume a Projection is a ProjectionImpl
 * without loss of generality.
 *
 * @author John Caron
 * @see Projection
 * @see ProjectionImpl
 */

public class ProjectionAdapter extends ProjectionImpl {

  /**
   * projection to adapt
   */
  private Projection proj;

  /**
   * Create a ProjectionImpl from the projection
   *
   * @param proj projection
   * @return a ProjectionImpl representing the projection
   */
  static public ProjectionImpl factory(Projection proj) {
    if (proj instanceof ProjectionImpl) {
      return (ProjectionImpl) proj;
    }
    return new ProjectionAdapter(proj);
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new ProjectionAdapter(proj);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Create a new ProjectionImpl from a Projection
   *
   * @param proj projection to use
   */
  public ProjectionAdapter(Projection proj) {
    super(proj.getName(), proj instanceof LatLonProjection);
    this.proj = proj;
  }

  /**
   * Get the class name
   *
   * @return the class name
   */
  public String getClassName() {
    return proj.getClassName();
  }


  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return proj.paramsToString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionAdapter that = (ProjectionAdapter) o;
    return proj.equals(that.proj);
  }

  @Override
  public int hashCode() {
    return proj.hashCode();
  }

  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latlon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  public ProjectionPoint latLonToProj(LatLonPoint latlon,
                                      ProjectionPointImpl result) {
    return proj.latLonToProj(latlon, result);
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint world,
                                  LatLonPointImpl result) {
    return proj.projToLatLon(world, result);
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return proj.crossSeam(pt1, pt2);
  }

  /**
   * Get a reasonable bounding box for this projection.
   *
   * @return reasonable bounding box
   */
  public ProjectionRect getDefaultMapArea() {
    return proj.getDefaultMapArea();
  }

}

/*
 *  Change History:
 *  $Log: ProjectionAdapter.java,v $
 *  Revision 1.17  2006/11/18 19:03:23  dmurray
 *  jindent
 *
 *  Revision 1.16  2005/05/13 18:29:18  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.15  2005/05/13 11:14:10  jeffmc
 *  Snapshot
 *
 *  Revision 1.14  2004/09/22 21:19:52  caron
 *  use Parameter, not Attribute; remove nc2 dependencies
 *
 *  Revision 1.13  2004/07/30 17:22:20  dmurray
 *  Jindent and doclint
 *
 *  Revision 1.12  2004/02/27 21:21:40  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.11  2004/01/29 17:35:00  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.10  2003/07/12 23:08:59  caron
 *  add cvs headers, trailers
 *
 */








