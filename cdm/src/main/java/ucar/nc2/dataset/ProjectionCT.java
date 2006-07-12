// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset;

import ucar.unidata.geoloc.*;
import ucar.unidata.util.Parameter;

import java.util.*;

/**
 * A Projection CoordinateTransform is a function from (GeoX, GeoY) -> (Lat, Lon).
 *
 * @author caron
 * @version $Revision$ $Date$
 */

public class ProjectionCT extends CoordinateTransform {
   private ProjectionImpl proj;

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

  /** get the Projection function */
  public ProjectionImpl getProjection() { return proj; }
}