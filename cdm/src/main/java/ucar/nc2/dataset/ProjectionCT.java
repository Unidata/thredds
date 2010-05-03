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

package ucar.nc2.dataset;

import net.jcip.annotations.Immutable;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.Parameter;

import java.util.*;

/**
 * A Projection CoordinateTransform is a function from (GeoX, GeoY) -> (Lat, Lon).
 *
 * @author caron
 */

@Immutable
public class ProjectionCT extends CoordinateTransform {
   private final ProjectionImpl proj;

  /**
   * Create a Projection Coordinate Transform.
   * @param name name of transform, must be unique within the NcML.
   * @param authority naming authority.
   * @param proj projection function.
   */
  public ProjectionCT (String name, String authority, ProjectionImpl proj) {
    super( name, authority, TransformType.Projection);
    this.proj = proj;

    List list = proj.getProjectionParameters();
    for (int i=0; i<list.size(); i++) {
      addParameter((Parameter) list.get(i));
    }
  }

  /** get the Projection function
   * @return the Projection
   */
  public ProjectionImpl getProjection() { return proj; }
}