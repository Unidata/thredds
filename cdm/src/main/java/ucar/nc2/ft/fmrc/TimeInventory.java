/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

/**
 * Extract time coordinates from a FmrcInvLite.Gridset.
 * For 1D time datasets.
 *
 * @author caron
 * @since Apr 15, 2010
 */
public interface TimeInventory {
  public String getName();

  public int getTimeLength(FmrcInvLite.Gridset gridset);
  public FmrcInvLite.ValueB getTimeCoords(FmrcInvLite.Gridset gridset);
  public double[] getRunTimeCoords(FmrcInvLite.Gridset gridset);
  public double[] getOffsetCoords(FmrcInvLite.Gridset gridset);  

  public Instance getInstance( FmrcInvLite.Gridset.Grid gridLite, int timeIdx);

  public interface Instance {
    public String getDatasetLocation();
    public int getDatasetIndex();
  }

}
