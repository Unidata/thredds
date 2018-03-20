/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import ucar.unidata.geoloc.ProjectionImpl;

/** Used to notify listeners that there is a new Projection.
 * @author John Caron
 */
public class NewProjectionEvent extends java.util.EventObject {
  private ProjectionImpl project;

  public NewProjectionEvent(Object source, ProjectionImpl proj) {
    super(source);
    this.project = proj;
  }

  public ProjectionImpl getProjection() {
    return project;
  }

}
