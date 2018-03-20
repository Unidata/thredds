/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;
/**
 * Used to notify listeners that there is a new geographic area selection.
 * @author John Caron
 */
public class GeoSelectionEvent extends java.util.EventObject {
  private ucar.unidata.geoloc.ProjectionRect pr;

  public GeoSelectionEvent(Object source, ucar.unidata.geoloc.ProjectionRect pr) {
    super(source);
    this.pr = pr;
  }

  public ucar.unidata.geoloc.ProjectionRect getProjectionRect() { return pr; }
}

