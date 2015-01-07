/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import net.jcip.annotations.Immutable;

import java.util.*;

/**
 * Generalized Vertical Coordinate.
 *
 * @author caron
 * @since 3/31/11
 */
@Immutable
public class VertCoord { // implements Comparable<VertCoord> {

  static public void assignVertNames(List<VertCoord> vertCoords, GribTables tables) {
    List<VertCoord> temp = new ArrayList<>(vertCoords); // dont change order of original !!!!!

    // assign name
    for (VertCoord vc : temp) {
      String shortName = tables.getLevelNameShort(vc.getCode());
      if (vc.isLayer()) shortName = shortName + "_layer";
      vc.setName(shortName);
    }

    // sort by name
    Collections.sort(temp, new Comparator<VertCoord>() {
      public int compare(VertCoord o1, VertCoord o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    // disambiguate names
    String lastName = null;
    int count = 0;
    for (VertCoord vc : temp) {
      String name = vc.getName();
      if ((lastName == null) || !lastName.equals(name)) {
        count = 0;
      } else {
        count++;
        vc.setName(name + count);
      }
      lastName = name;
    }
  }

  private String name;

  private final List<VertCoord.Level> coords;
  private final VertUnit unit;
  private final boolean isLayer;

  /* public VertCoord(int code, List<VertCoord.Level> coords, boolean isLayer) {
    this.coords = coords;
    this.isLayer = isLayer;
    this.unit = Grib2Utils.getLevelUnit(code);
  }  */

  public VertCoord(List<VertCoord.Level> coords, VertUnit unit, boolean isLayer) {
    this.coords = coords;
    this.unit = unit;
    this.isLayer = isLayer;
  }

  /**
   * vert coordinates are used when nlevels > 1, otherwise use isVerticalCoordinate
   *
   * @return if vert dimension should be used
   */
  public boolean isVertDimensionUsed() {
    return (coords.size() == 1) ? unit.isVerticalCoordinate() : true;
  }

  public boolean isLayer() {
    return isLayer;
  }

  public boolean isPositiveUp() {
    return unit.isPositiveUp();
  }

  public List<Level> getCoords() {
    return coords;
  }

  public int getCode() {
    return unit.getCode();
  }

  public String getUnits() {
    return unit.getUnits();
  }

  public int getSize() {
    return coords.size();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name == null ? "" : name;
  }

  public boolean equalsData(VertCoord other) {
    if (unit.getCode() != other.unit.getCode())
      return false;

    if (isLayer != other.isLayer)
      return false;

    if (coords.size() != other.coords.size())
      return false;

    for (int i = 0; i < coords.size(); i++) {
      if (!coords.get(i).equals(other.coords.get(i)))
        return false;
    }

    return true;
  }

  public int findIdx(Level coord) {
    for (int i = 0; i < coords.size(); i++) { // LOOK linear search
      if (coords.get(i).equals(coord))
        return i;
    }
    return -1;
  }

  @Override
  public String toString() {
    Formatter out = new Formatter();
    out.format("(3D=%s) code=%d = ", isVertDimensionUsed(), getCode());
    for (Level lev : coords) out.format("%s, ", lev.toString(isLayer));
    out.format("units='%s' isLayer=%s", getUnits(), isLayer);
    return out.toString();
  }

  public String showCoords() {
    Formatter out = new Formatter();
    for (Level lev : coords) out.format("%s, ", lev.toString(isLayer));
    return out.toString();
  }

  ///////////////////////////////////////////////////////

  static public int findCoord(List<VertCoord> vertCoords, VertCoord want) {
    if (want == null) return -1;

    for (int i = 0; i < vertCoords.size(); i++) {
      if (want.equalsData(vertCoords.get(i)))
        return i;
    }

    // make a new one
    vertCoords.add(want);
    return vertCoords.size() - 1;
  }

  @Immutable
  static public class Level implements Comparable<Level> {
    final double value1;
    final double value2;
    final double mid;
    final boolean isLayer;

    /* public Level(double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      this.mid = (value2 == 0 || value2 == GribNumbers.UNDEFINEDD) ? value1 : (value1 + value2) / 2;
    } */

    public Level(double value1, double value2, boolean isLayer) {
      this.value1 = value1;
      this.value2 = value2;
      this.mid = (Double.compare(value2, 0.0) == 0 || GribNumbers.isUndefined(value2)) ? value1 : (value1 + value2) / 2;
      this.isLayer = isLayer;
    }

    public double getValue1() {
      return value1;
    }

    public double getValue2() {
      return value2;
    }

    public boolean isLayer() {
      return isLayer;
    }

    @Override
    public int compareTo(Level o) {
      if (mid < o.mid) return -1;
      if (mid > o.mid) return 1;
      return 0;
      //int c1 = Double.compare(value2, o.value2);
      //return (c1 == 0) ? Double.compare(value1, o.value1) : c1;
    }

    @Override
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof Level))
        return false;
      Level other = (Level) oo;
      return (ucar.nc2.util.Misc.closeEnough(value1, other.value1) && ucar.nc2.util.Misc.closeEnough(value2, other.value2));
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = value1 != +0.0d ? Double.doubleToLongBits(value1) : 0L;
      result = (int) (temp ^ (temp >>> 32));
      temp = value2 != +0.0d ? Double.doubleToLongBits(value2) : 0L;
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    public String toString() {
      Formatter out = new Formatter();
      if (isLayer)
        out.format("(%f,%f)", value1, value2);
      else
        out.format("%f", value1);
      return out.toString();
    }

    public String toString(boolean isLayer) {
      Formatter out = new Formatter();
      if (isLayer)
        out.format("(%f,%f)", value1, value2);
      else
        out.format("%f", value1);
      return out.toString();
    }
  }

  @Immutable
  static public interface VertUnit {

    public int getCode();

    public String getUnits();

    public String getDatum();

    public boolean isLayer();

    public boolean isPositiveUp();

    public boolean isVerticalCoordinate();
  }
}

