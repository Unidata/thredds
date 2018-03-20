/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grid;

import ucar.nc2.*;
import java.util.*;

/**
 * Handles the Ensemble coordinate dimension.
 */
public abstract class GridEnsembleCoord {
  static private org.slf4j.Logger log =  org.slf4j.LoggerFactory.getLogger(GridEnsembleCoord.class);

  protected List<EnsCoord> ensCoords;
  protected int seq = 0;

  static protected class EnsCoord implements Comparable<EnsCoord> {
    public int number, type;

    public EnsCoord(int number, int type) {
      this.number = number;
      this.type = type;
    }

    @Override
    public int compareTo(EnsCoord o) {
      int r = number - o.number;
      return (r == 0) ? type - o.type : r;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      EnsCoord ensCoord = (EnsCoord) o;

      if (number != ensCoord.number) return false;
      if (type != ensCoord.type) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return 1000 * number + type;
    }

    @Override
    public String toString() {
      return "number=" + number +", type=" + type;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GridEnsembleCoord that = (GridEnsembleCoord) o;
    return ensCoords.equals(that.ensCoords);
  }

  @Override
  public int hashCode() {
    return ensCoords.hashCode();
  }

  /**
   * Set the sequence number
   *
   * @param seq the sequence number
   */
  public void setSequence(int seq) {
    this.seq = seq;
  }

  /**
   * Get the name
   *
   * @return the name
   */
  public String getName() {
    return (seq == 0) ? "ens" : "ens" + seq;
  }

  /**
   * Add this as a dimension to a netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  public void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
    ncfile.addDimension(g, new Dimension(getName(), getNEnsembles(), true));
  }

  protected void addToNetcdfFile(NetcdfFile ncfile, Group g) {
    // noop
  }

  /**
   * Get the number of Ensembles
   *
   * @return the number of Ensembles
   */
  public int getNEnsembles() {
    return ensCoords.size();
  }

}
