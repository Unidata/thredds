/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.Parameter;

import javax.annotation.concurrent.Immutable;

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

    for (Parameter p : proj.getProjectionParameters()) {
      addParameter( p);
    }
  }

  /** get the Projection function
   * @return the Projection
   */
  public ProjectionImpl getProjection() { return proj; }
}
