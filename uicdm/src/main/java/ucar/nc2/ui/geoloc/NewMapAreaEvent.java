/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import ucar.unidata.geoloc.ProjectionRect;

/**
 * Used to notify listeners that there is a new world bounding box.
 * @author John Caron
 */
public class NewMapAreaEvent extends java.util.EventObject {
  private ProjectionRect mapArea;

  public NewMapAreaEvent(Object source, ProjectionRect mapArea) {
    super(source);
    this.mapArea = mapArea;
  }

  public ProjectionRect getMapArea() {
    return mapArea;
  }
}
