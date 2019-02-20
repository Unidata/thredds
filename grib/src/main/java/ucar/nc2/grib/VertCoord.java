/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import javax.annotation.Nonnull;
import ucar.nc2.util.Misc;

import javax.annotation.concurrent.Immutable;

import java.util.*;

/**
 * Generalized Vertical Coordinate.
 *
 * @author caron
 * @since 3/31/11
 */
@Immutable
public class VertCoord {

  public static void assignVertNames(List<VertCoord> vertCoords, GribTables tables) {
    List<VertCoord> temp = new ArrayList<>(vertCoords); // dont change order of original !!!!!

    // assign name
    for (VertCoord vc : temp) {
      String shortName = tables.getLevelNameShort(vc.getCode());
      if (vc.isLayer()) shortName = shortName + "_layer";
      vc.setName(shortName);
    }

    // sort by name
    temp.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));

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
    return (coords.size() != 1) || unit.isVerticalCoordinate();
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
      if (!coords.get(i).nearlyEquals(other.coords.get(i)))
        return false;
    }

    return true;
  }

  @Override
  public String toString() {
    try (Formatter out = new Formatter()) {
      out.format("(3D=%s) code=%d = ", isVertDimensionUsed(), getCode());
      for (Level lev : coords) {
        out.format("%s, ", lev.toString(isLayer));
      }
      out.format("units='%s' isLayer=%s", getUnits(), isLayer);
      return out.toString();
    }
  }

  public String showCoords() {
    try (Formatter out = new Formatter()) {
      for (Level lev : coords) {
        out.format("%s, ", lev.toString(isLayer));
      }
      return out.toString();
    }
  }

  ///////////////////////////////////////////////////////

  public static int findCoord(List<VertCoord> vertCoords, VertCoord want) {
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
  public static class Level implements Comparable<Level> {
    final double value1;
    final double value2;
    final double mid;
    final boolean isLayer;

    public Level(double value1) {
      this.value1 = value1;
      this.value2 = GribNumbers.UNDEFINEDD;
      this.mid = value1;
      this.isLayer = false;
    }

    public Level(double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      this.mid = (Double.compare(value2, 0.0) == 0 || GribNumbers.isUndefined(value2)) ? value1 : (value1 + value2) / 2;
      this.isLayer = true;
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
    public int compareTo(@Nonnull Level o) {
      return Double.compare(mid, o.mid);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Level level = (Level) o;
      if (Double.compare(level.value1, value1) != 0) return false;
      if (Double.compare(level.value2, value2) != 0) return false;
      return isLayer == level.isLayer;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(value1);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(value2);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + (isLayer ? 1 : 0);
      return result;
    }

    // cannot do approx equals and be consistent with hashCode, so make seperate call
    public boolean nearlyEquals(Level other) {
      return Misc.nearlyEquals(value1, other.value1) && Misc.nearlyEquals(value2, other.value2);
    }

    public String toString() {
      try (Formatter out = new Formatter()) {
        if (isLayer) {
          out.format("(%f,%f)", value1, value2);
        } else {
          out.format("%f", value1);
        }
        return out.toString();
      }
    }

    public String toString(boolean isLayer) {
      try (Formatter out = new Formatter()) {
        if (isLayer) {
          out.format("(%f,%f)", value1, value2);
        } else {
          out.format("%f", value1);
        }
        return out.toString();
      }
    }
  }

  @Immutable
  public interface VertUnit {
    int getCode();
    String getUnits();
    String getDesc();
    String getDatum();
    boolean isLayer();
    boolean isPositiveUp();
    boolean isVerticalCoordinate();
  }
}
