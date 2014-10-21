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
